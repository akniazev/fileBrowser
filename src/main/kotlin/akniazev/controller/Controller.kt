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
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.ZoneId
import java.util.*
import javax.imageio.ImageIO
import javax.swing.SwingWorker
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


    fun listChildren(file: DisplayableFile) = when(file) {
        is SystemFile -> {
            val resultList = mutableListOf<DisplayableFile>()
            Files.walkFileTree(file.path, emptySet(), 1, SystemFileVisitor(resultList))
            resultList
        }
        is ZipFile -> {
            val resultList = mutableListOf<DisplayableFile>()
            Files.walkFileTree(file.file, emptySet(), 1, ZipFileVisitor(resultList))
            resultList
        }
        is FtpFile -> file.file.children.asSequence().map { println(it.name); FtpFile(it) }.toList()
        else -> throw IllegalArgumentException()
    }

    fun navigateTo(file: DisplayableFile, children: List<DisplayableFile>) {
        currentFile = file
        val (parent, path) = when(file) {
            is SystemFile -> Pair(file.parent, file.path.toString())
            is ZipFile -> Pair(if (file.isRoot) zipExit else file.parent, file.file.toString())
            is FtpFile -> Pair(file.parent, file.file.publicURIString)
            else -> throw IllegalArgumentException()
        }
        view.updateFileList(parent, children)
        view.updateAddress(path, file !is ZipFile)
        view.updateNavigation(navigateBackStack.isNotEmpty(),
                              navigateForwardStack.isNotEmpty(),
                              file is ZipFile || !file.isRoot)
    }

    override fun navigateTo(file: DisplayableFile) {
        currentFile = file
        when(file) {
            is SystemFile -> {
                val resultList = mutableListOf<DisplayableFile>()
                Files.walkFileTree(file.path, emptySet(), 1, SystemFileVisitor(resultList))
                view.updateFileList(file.parent, resultList)

                view.updateAddress(file.path.toString(), true)
                view.updateNavigation(navigateBackStack.isNotEmpty(), navigateForwardStack.isNotEmpty(), !file.isRoot)
            }
            is ZipFile -> {
                val parent = if (file.isRoot) zipExit else file.parent
                val resultList = mutableListOf<DisplayableFile>()
                Files.walkFileTree(file.file, emptySet(), 1, ZipFileVisitor(resultList))
                view.updateFileList(parent, resultList)

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
                object : SwingWorker<List<DisplayableFile>, Int>() {
                    override fun doInBackground(): List<DisplayableFile> {
                        return listChildren(file)
                    }
                    override fun done() {
                        navigateTo(file, get())
                    }
                }.execute()
            } else if (file is SystemFile) {
                if (file.extension == "zip") {
                    zipExit = file.parent
                    navigateBackStack.push(zipExit)
                    val zipRoot = getFileSystem(file.path).getPath("/")
                    val resultList = mutableListOf<DisplayableFile>()
                    Files.walkFileTree(zipRoot, emptySet(), 1, ZipFileVisitor(resultList))
                    view.updateFileList(zipExit, resultList)

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
                    if (type == FileType.TEXT) {
                        val result = readContent(Files.newBufferedReader(path))
                        view.previewText(file.name, file.size, file.lastModified, result)
                    } else if (type == FileType.IMAGE) {
                        val image = ImageIO.read(path?.toUri()?.toURL())
                        view.previewImage(file.name, file.size, file.lastModified, getScaledImage(image))
                    } else {
                        view.previewFile(file.name, file.size, file.lastModified)
                    }
                }
                is FtpFile -> {
                    if (type == FileType.TEXT) {
                        val result = readContent(BufferedReader(InputStreamReader(file.file.content.inputStream)))
                        view.previewText(file.name, file.size, file.lastModified, result)
                    } else if (type == FileType.IMAGE) {
                        val image = ImageIO.read(file.file.content.inputStream)
                        view.previewImage(file.name, file.size, file.lastModified, getScaledImage(image))
                    } else {
                        view.previewFile(file.name, file.size, file.lastModified)
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