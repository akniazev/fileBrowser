package akniazev.controller

import akniazev.common.*
import akniazev.common.View
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.VFS
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder
import java.awt.Desktop
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.nio.CharBuffer
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributeView
import java.time.Instant
import java.time.ZoneId
import java.util.*
import javax.imageio.ImageIO
import kotlin.streams.asSequence

typealias FtpFS = org.apache.commons.vfs2.FileSystem

class ControllerImpl : Controller {

    override lateinit var view: View
    private var zipExit: SystemFile? = null
    private var ftpConnected: Boolean = false
    private var ftpFileSystem: FtpFS? = null
    private val navigateBackStack: Deque<DisplayableFile> = EvictingStack(10)
    private val navigateForwardStack: Deque<DisplayableFile> = EvictingStack(10)
    private val buffer: CharBuffer = CharBuffer.allocate(150)
    private var currentFile: DisplayableFile? = null
    private val desktop: Desktop? = if (Desktop.isDesktopSupported()
            && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) Desktop.getDesktop() else null


    override fun navigateTo(file: DisplayableFile) {
        currentFile = file
        when(file) {
            is SystemFile -> {
                view.updateFileList(file.parent, Files.list(file.path).asSequence().map(::SystemFile).toList())

                view.updateAddress(file.path.toString(), true)
                view.updateNavigation(navigateBackStack.isNotEmpty(), navigateForwardStack.isNotEmpty(), !file.isRoot)
            }
            is ZipFile -> {
                val parent = if (file.isRoot) zipExit else file.parent
                val list = Files.list(file.file).asSequence().map(::ZipFile).toList()
                view.updateFileList(parent, list)

                view.updateAddress(file.file.toString(), false)
                view.updateNavigation(navigateBackStack.isNotEmpty(), navigateForwardStack.isNotEmpty(), true)
            }
            is FtpFile -> {
                val list = file.file.children.asSequence().map(::FtpFile).toList()
                view.updateFileList(file.parent, list)

                view.updateAddress(file.file.publicURIString, false)
                view.updateNavigation(navigateBackStack.isNotEmpty(), navigateForwardStack.isNotEmpty(), !file.isRoot)
            }
        }
    }

    override fun connectToFtp(host: String, port: String, user: String, pass: String) {
        val builder = StringBuilder("ftp://")
        if (user != "") {
            builder.append(user)
            if(pass != "") builder.append(":$pass")
            builder.append('@')
        }
        builder.append(host)
        if (port != "") builder.append(":$port/")

        val url = builder.toString()
        val opts = FileSystemOptions()
        FtpFileSystemConfigBuilder.getInstance().setPassiveMode(opts, true)
        val file = VFS.getManager().resolveFile(url, opts)
        ftpConnected = true
        ftpFileSystem = file.fileSystem
        navigateTo(FtpFile(file))
        view.ftpConnected()
    }

    override fun handleTableClick(clickCount: Int, file: DisplayableFile) {
        val type = file.type
        if (clickCount == 2) {
            if (file.isDirectory) {
                navigateForwardStack.clear()
                if (!file.isRoot) {
                    if (currentFile != null) navigateBackStack.push(currentFile)
                }
                navigateTo(file)
            } else if (file is SystemFile) {
                if (file.extension == "zip") {
                    zipExit = file.parent
                    navigateBackStack.push(zipExit)
                    val zipRoot = getFileSystem(file.path).getPath("/")
                    val list = Files.list(zipRoot).asSequence().map(::ZipFile).toList()
                    view.updateFileList(zipExit, list)

                    view.updateAddress("/", false)
                    view.updateNavigation(navigateBackStack.isNotEmpty(), navigateForwardStack.isNotEmpty(), true)

                } else {
                    desktop?.open(file.path.toFile())
                }
            }
        } else if (clickCount == 1) {
            when (file) {
                is SystemFile, is ZipFile -> {
                    val path = (file as? SystemFile)?.path ?: (file as? ZipFile)?.file
                    val attrs = Files.getFileAttributeView(path, BasicFileAttributeView::class.java).readAttributes()
                    val lastModifiedTime = attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault())
                    val size = attrs.size()

                    if (type == FileType.TEXT) {
                        val result = readContent(Files.newBufferedReader(path))
                        view.previewText(file.name, size, lastModifiedTime, result)
                    } else if (type == FileType.IMAGE) {
                        val image = ImageIO.read(path?.toUri()?.toURL())
                        view.previewImage(file.name, size, lastModifiedTime, getScaledImage(image))
                    } else {
                        view.previewFile(file.name, size, lastModifiedTime)
                    }
                }
                is FtpFile -> {
                    val content = file.file.content
                    val size = if (file.isDirectory) 0 else content.size
                    val lastModifiedTime = Instant.ofEpochMilli(content.lastModifiedTime).atZone(ZoneId.systemDefault())

                    if (type == FileType.TEXT) {
                        val result = readContent(BufferedReader(InputStreamReader(file.file.content.inputStream)))
                        view.previewText(file.name, size, lastModifiedTime, result)
                    } else if (type == FileType.IMAGE) {
                        val image = ImageIO.read(file.file.content.inputStream)
                        view.previewImage(file.name, size, lastModifiedTime, getScaledImage(image))
                    } else {
                        view.previewFile(file.name, size, lastModifiedTime)
                    }
                }
            }
        }
    }

    override fun tryNavigate(pathText: String): String {
        val normalized = pathText.replace('\\', '/')
        val path = Paths.get(normalized)
        if (Files.exists(path) && Files.isDirectory(path)) {
            val file = SystemFile(path)
            navigateBackStack.push(file)
            navigateForwardStack.clear()
            navigateTo(file)
        } else throw IncorrectPathException()
        return normalized
    }

    override fun navigateBack() {
        val newPath = navigateBackStack.pop()

        navigateForwardStack.push(currentFile)
        navigateTo(newPath)
    }

    override fun navigateForward() {
        val newPath = navigateForwardStack.pop()
        navigateBackStack.push(currentFile)
        navigateTo(newPath)
    }

    override fun navigateToRoot(path: Path) {
        val file = SystemFile(path)
        if (currentFile != null) navigateBackStack.push(currentFile)
        navigateTo(file)
    }

    override fun disconnectFtp() {
        if (ftpConnected) {
            VFS.getManager().closeFileSystem(ftpFileSystem)
            ftpConnected = false
        }
        view.ftpDisconnected()
    }

    override fun cleanup() {
        if (ftpConnected) {
            VFS.getManager().closeFileSystem(ftpFileSystem)
            ftpConnected = false
        }
    }

    private fun getFileSystem(path: Path): FileSystem {
        val uri = URI.create("jar:file:///${path.toString().replace("\\", "/")}")
        return try {
            FileSystems.getFileSystem(uri)
        } catch (e: FileSystemNotFoundException) {
            FileSystems.newFileSystem(uri, mapOf("encoding" to "CP866"))
        }
    }

    private fun readContent(br: BufferedReader) = br.use { reader ->
        val read = reader.read(buffer)
        buffer.rewind()
        if (read > 0) buffer.substring(0, read) else ""
    }
}