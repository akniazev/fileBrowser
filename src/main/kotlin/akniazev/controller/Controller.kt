package akniazev.controller

import akniazev.common.*
import akniazev.ui.TableModel
import akniazev.ui.View
import com.sun.nio.zipfs.ZipPath
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.VFS
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder
import java.awt.Desktop
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.net.URI
import java.nio.CharBuffer
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JTable
import kotlin.streams.asSequence

typealias FtpFS = org.apache.commons.vfs2.FileSystem


class ControllerImpl : Controller {

    private val desktop: Desktop?
    private var zipExit: SystemFile? = null
    private var ftpConnected: Boolean = false
    private var ftpFileSystem: FtpFS? = null
    private val navigateBackStack: Deque<DisplayableFile> = LinkedList()
    private val navigateForwardStack: Deque<DisplayableFile> = LinkedList()
    private val buffer: CharBuffer = CharBuffer.allocate(100)
    override lateinit var view: View



    init {
        desktop = if (Desktop.isDesktopSupported()
                      && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) Desktop.getDesktop()
                  else null
    }

    override fun navigateTo(path: Path, model: TableModel) {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                val parent = if (path.parent != null) SystemFile(path.parent) else null
                model.updateTable(parent, Files.list(path).asSequence().map(::SystemFile).toList())
            } else {
                val directoryPath = path.parent
                val parent = if (directoryPath.parent != null) SystemFile(directoryPath.parent) else null
                model.updateTable(parent, Files.list(directoryPath).asSequence().map(::SystemFile).toList())

            }
        }
    }

    override fun connectToFtp(host: String, port: String, user: String, pass: String, model: TableModel) {
        val url = "ftp://$user:$pass@$host:$port/"
        val opts = FileSystemOptions()
        FtpFileSystemConfigBuilder.getInstance().setPassiveMode(opts, true)

        val file = VFS.getManager().resolveFile(url, opts)
        ftpFileSystem = file.fileSystem

        val list = file.children.asSequence().map(::FtpFile).toList()
        model.updateTable(null, list)
    }

    override fun handleTableClick(e: MouseEvent) {
        val table = e.source as JTable
        val model = table.model as TableModel
        val file = model.files[table.selectedRow]
        val type = file.type


        if (e.clickCount == 2) {
            when(file) {
                is ZipFile -> {
                    // todo extract fun
                    if (file.isDirectory) {
                        val parent = if (file.isRoot) zipExit else ZipFile(file.parent!!)
                        val list = Files.list(file.file).asSequence().map(::ZipFile).toList()
                        model.updateTable(parent, list)
                    }
                }
                is SystemFile -> {
                    if (file.isDirectory) {
                        val parent = if (file.isRoot) null else SystemFile(file.parent!!)
                        val list = Files.list(file.path).asSequence().map(::SystemFile).toList()
                        model.updateTable(parent, list)
                    } else if (file.extension == "zip") {
                        zipExit = SystemFile(file.parent!!)
                        val zipRoot = getFileSystem(file.path).getPath("/")
                        val list = Files.list(zipRoot).asSequence().map(::ZipFile).toList()
                        model.updateTable(zipExit, list)
                    }
                }
                is FtpFile -> {
                    if (file.isDirectory) {
                        val parent = if (file.isRoot) null else FtpFile(file.parent!!)
                        val list = file.file.children.asSequence().map(::FtpFile).toList()
                        model.updateTable(parent, list)
                    }
                }
                else -> throw IllegalStateException("Unknown file type")
            }
        } else if (type == FileType.TEXT || type == FileType.IMAGE) {
            when (file) {
                is SystemFile, is ZipFile -> {
                    val path = (file as? SystemFile)?.path ?: (file as? ZipFile)?.file
                    val attrs = Files.getFileAttributeView(path, BasicFileAttributeView::class.java).readAttributes()
                    val createdTime = attrs.creationTime().toSystemDateTime() // todo fix possible npe
                    val lastAccessedTime = attrs.lastAccessTime().toSystemDateTime()
                    val size = attrs.size()
                    println("created: $createdTime, accessed $lastAccessedTime, size $size")
                    if (type == FileType.TEXT) {
                        Files.newBufferedReader(path).use { reader ->
                            val read = reader.read(buffer)
                            buffer.rewind()
                            val result = buffer.substring(0, read)
                            println("From buffer: $result")
                            view.previewText(file.name, createdTime, lastAccessedTime, size, result)
                        }
                    } else {
                        val image = ImageIO.read(path?.toUri()?.toURL())
                        view.previewImage(file.name, createdTime, lastAccessedTime, size, getScaledImage(image))
                    }
                }
                is FtpFile -> {
//                    file.file.content
                }
            }
        }
    }

    override fun toPreviousLevel(model: TableModel) {
        val current = model.parentFile
        when (current) {
            is SystemFile -> {
                val parent = if (current.isRoot) null else SystemFile(current.parent!!)
                val list = Files.list(current.path).asSequence().map(::SystemFile).toList()
                model.updateTable(parent, list)
            }
            is ZipFile -> {
                if (current is SystemFile) {
                    val parent = if (current.isRoot) null else SystemFile(current.parent!!)
                    val list = Files.list(current.path).asSequence().map(::SystemFile).toList()
                    model.updateTable(parent, list)
                } else {
                    val parent = if (current.isRoot) zipExit else ZipFile(current.parent!!)
                    val list = Files.list(current.file).asSequence().map(::ZipFile).toList()
                    model.updateTable(parent, list)
                }
            }
            is FtpFile ->  {
                val parent = if (current.isRoot) null else FtpFile(current.parent!!)
                val list = current.file.children.asSequence().map(::FtpFile).toList()
                model.updateTable(parent, list)
            }
        }
    }


    override fun navigateBack(model: TableModel) {
        val newPath = navigateBackStack.pop()
        navigateForwardStack.push(newPath)
//        navigateTo(newPath.)
    }

    override fun navigateForward(model: TableModel) {
        TODO("not implemented")
    }

    private fun getFileSystem(path: Path): FileSystem {
        val uri = URI.create("jar:file:///${path.toString().replace("\\", "/")}")
        return try {
            FileSystems.getFileSystem(uri)
        } catch (e: FileSystemNotFoundException) {
            FileSystems.newFileSystem(uri, mapOf("encoding" to "CP866"))
        }
    }


    private fun getScaledImage(img: BufferedImage): BufferedImage {
        val result = BufferedImage(160, 160, BufferedImage.TYPE_INT_RGB)
        val graphics = result.createGraphics()
        graphics.drawImage(img, 0, 0, 160, 160, null)
        graphics.dispose()
        return result
    }

}



interface Controller {
    var view: View
    fun handleTableClick(e: MouseEvent)
    fun toPreviousLevel(model: TableModel)
    fun connectToFtp(host: String, port: String, user: String, pass: String, model: TableModel)
    fun navigateTo(path: Path, model: TableModel)
    fun navigateBack(model: TableModel)
    fun navigateForward(model: TableModel)
}

fun FileTime.toSystemDateTime(): ZonedDateTime = this.toInstant().atZone(ZoneId.systemDefault())
