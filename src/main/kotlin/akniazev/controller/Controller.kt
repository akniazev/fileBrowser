package akniazev.controller

import akniazev.common.*
import akniazev.ui.TableModel
import akniazev.common.View
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.VFS
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder
import java.awt.Desktop
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.nio.CharBuffer
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributeView
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JTable
import kotlin.streams.asSequence

typealias FtpFS = org.apache.commons.vfs2.FileSystem
typealias FileInfo = Pair<Long, ZonedDateTime>


class ControllerImpl : Controller {

    private val desktop: Desktop?
    private var zipExit: SystemFile? = null
    private var ftpConnected: Boolean = false
    private var ftpFileSystem: FtpFS? = null
    private val navigateBackStack: Deque<DisplayableFile> = LinkedList()
    private val navigateForwardStack: Deque<DisplayableFile> = LinkedList()
    private val buffer: CharBuffer = CharBuffer.allocate(150)
    override lateinit var view: View


    init {
        desktop = if (Desktop.isDesktopSupported()
                      && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) Desktop.getDesktop()
                  else null
    }


    override fun navigateTo(file: DisplayableFile) {
        when(file) {
            is FtpFile -> {
                val list = file.file.children.asSequence().map(::FtpFile).toList()
                view.updateFileList(file.parent, list)
            }
            is SystemFile -> {
                if (Files.isDirectory(file.path)) {
                    view.updateFileList(file.parent, Files.list(file.path).asSequence().map(::SystemFile).toList())
                } else {
                    val directoryPath = file.parent!!
                    view.updateFileList(directoryPath.parent, Files.list(directoryPath.path).asSequence().map(::SystemFile).toList())
                    // todo set selection
                }
            }
            is ZipFile -> {
                val parent = if (file.isRoot) zipExit else file.parent
                val list = Files.list(file.file).asSequence().map(::ZipFile).toList()
                view.updateFileList(parent, list)
            }
        }
    }

    override fun connectToFtp(host: String, port: String, user: String, pass: String) {
        val url = "ftp://$user:$pass@$host:$port/"
        val opts = FileSystemOptions()
        FtpFileSystemConfigBuilder.getInstance().setPassiveMode(opts, true)
        val file = VFS.getManager().resolveFile(url, opts)
        ftpConnected = true
        ftpFileSystem = file.fileSystem
        navigateTo(FtpFile(file))
    }

    override fun handleTableClick(clickCount: Int, file: DisplayableFile) {
        val type = file.type
        if (clickCount == 2) {
            if (file.isDirectory) {
                navigateForwardStack.clear()
                if (!file.isRoot) navigateBackStack.push(file)
                navigateTo(file)
            } else if (file is SystemFile) {
                if (file.extension == "zip") {
                    zipExit = file.parent
                    navigateBackStack.push(zipExit)
                    val zipRoot = getFileSystem(file.path).getPath("/")
                    val list = Files.list(zipRoot).asSequence().map(::ZipFile).toList()
                    view.updateFileList(zipExit, list)
                } else {
                    desktop?.open(file.path.toFile())
                }
            }
        } else if (clickCount == 1) {
//            val (size, lastModifiedTime) = when(file) {
//                is SystemFile, is ZipFile -> {
//                    val path = (file as? SystemFile)?.path ?: (file as? ZipFile)?.file
//                    val attrs = Files.getFileAttributeView(path, BasicFileAttributeView::class.java).readAttributes()
//                    val lastModifiedTime = attrs.lastModifiedTime().toSystemDateTime()
//                    val size = attrs.size()
//                    FileInfo(size, lastModifiedTime)
//                }
//                is FtpFile -> {
//                    val content = file.file.content
//                    val size = content.size
//                    val lastModifiedTime = Instant.ofEpochMilli(content.lastModifiedTime).atZone(ZoneId.systemDefault())
//                    FileInfo(size, lastModifiedTime)
//                }
//                else -> throw IllegalArgumentException()
//            }

//            if (type == FileType.TEXT) {
//                val content = when(file) {
//                    is SystemFile, is ZipFile -> readContent(Files.newBufferedReader(path))
//                    is FtpFile -> readContent(BufferedReader(InputStreamReader(file.file.content.inputStream)))
//                }
//
//            }

            when (file) {
                is SystemFile, is ZipFile -> {
                    val path = (file as? SystemFile)?.path ?: (file as? ZipFile)?.file
                    val attrs = Files.getFileAttributeView(path, BasicFileAttributeView::class.java).readAttributes()
                    val lastModifiedTime = attrs.lastModifiedTime().toSystemDateTime()
                    val size = attrs.size()
                    if (type == FileType.TEXT) {
                        val result = readContent(Files.newBufferedReader(path))
                        view.previewText(file.name, size, lastModifiedTime, result)
                    } else if (type == FileType.IMAGE) {
                        val image = ImageIO.read(path?.toUri()?.toURL())
                        view.previewImage(file.name, size, lastModifiedTime, getScaledImage(image))
                    } else {

                    }
                }
                is FtpFile -> {
                    val content = file.file.content
                    val size = content.size
                    val lastModifiedTime = Instant.ofEpochMilli(content.lastModifiedTime).atZone(ZoneId.systemDefault())
                    if (type == FileType.TEXT) {
                        val result = readContent(BufferedReader(InputStreamReader(file.file.content.inputStream)))
                        view.previewText(file.name, size, lastModifiedTime, result)
                    } else if (type == FileType.IMAGE) {
                        val image = ImageIO.read(file.file.content.inputStream)
                        view.previewImage(file.name, size, lastModifiedTime, getScaledImage(image))
                    } else {

                    }
                }
            }
        }
    }

    override fun tryNavigate(pathText: String): String {
        val normalized = pathText.replace('\\', '/')    // todo ftp
        val path = Paths.get(normalized)
        if (Files.exists(path)) {
            val file = SystemFile(path)
            navigateBackStack.push(file)
            navigateForwardStack.clear()
            navigateTo(file)
        } else throw IllegalArgumentException("Incorrect path")
        return normalized
    }

    override fun navigateBack() {
        val newPath = navigateBackStack.pop()
        navigateForwardStack.push(newPath)
        navigateTo(newPath)
        // todo set buttons status
    }

    override fun navigateForward() {
        val newPath = navigateForwardStack.pop()
        navigateBackStack.push(newPath)
        navigateTo(newPath)
        // todo set buttons status
    }

    override fun cleanup() {
        if (ftpConnected) {
            VFS.getManager().closeFileSystem(ftpFileSystem)
            println("ftp fs closed")
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
        buffer.substring(0, read)
    }
}