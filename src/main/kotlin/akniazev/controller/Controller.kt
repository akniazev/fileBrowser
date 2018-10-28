package akniazev.controller

import akniazev.common.DisplayableFile
import akniazev.common.FtpFile
import akniazev.common.SystemFile
import akniazev.common.ZipFile
import akniazev.ui.TableModel
import com.sun.nio.zipfs.ZipPath
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.VFS
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder
import java.awt.Desktop
import java.awt.event.MouseEvent
import java.net.URI
import java.nio.file.*
import javax.swing.JTable
import kotlin.streams.asSequence

typealias FtpFS = org.apache.commons.vfs2.FileSystem?


class ControllerImpl : Controller {

    private val desktop: Desktop?
    private var zipExit: SystemFile? = null
    private var ftpConnected = false
    private var ftpFileSystem: FtpFS = null


    init {
        desktop = if (Desktop.isDesktopSupported()
                      && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) Desktop.getDesktop()
                  else null
    }

    override fun changeRoot(path: Path, model: TableModel) {
        model.updateTable(null, Files.list(path).asSequence().map(::SystemFile).toList())
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

    override fun handleTableClick(e: MouseEvent?) {
        if (e?.clickCount == 2) {
            val table = e.source as JTable
            val model = table.model as TableModel
            val file = model.files[table.selectedRow]

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



    private fun getFileSystem(path: Path): FileSystem {
        val uri = URI.create("jar:file:///${path.toString().replace("\\", "/")}")
        return try {
            FileSystems.getFileSystem(uri)
        } catch (e: FileSystemNotFoundException) {
            FileSystems.newFileSystem(uri, mapOf("encoding" to "CP866"))
        }
    }

}



interface Controller {
    fun handleTableClick(e: MouseEvent?)
    fun toPreviousLevel(model: TableModel)
    fun connectToFtp(host: String, port: String, user: String, pass: String, model: TableModel)
    fun changeRoot(path: Path, model: TableModel)
}

