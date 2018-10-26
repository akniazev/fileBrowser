package akniazev.controller

import akniazev.ui.TableModel
import com.sun.nio.zipfs.ZipPath
import java.awt.Desktop
import java.awt.event.MouseEvent
import java.net.URI
import java.nio.file.*
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.swing.JTable


class ControllerImpl : Controller {

    private val desktop: Desktop?
    private var zipExit: Path? = null

    init {
        desktop = if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) Desktop.getDesktop() else null

    }

    override fun handleTableClick(e: MouseEvent?) {
        if (e?.clickCount == 2) {
            val table = e.source as JTable
            val model = table.model as TableModel
            val path = model.paths[table.selectedRow]
            println(path)

            if (path is ZipPath) {
                val parent = if ((path.nameCount > 0)) path.parent else zipExit
                val parentStream = if (parent != null) Stream.of(parent) else Stream.empty()
                val list = Stream.concat(parentStream, Files.list(path)).collect(Collectors.toList())
                println(list)
                model.paths = list
            } else if (path.toFile().isDirectory) {
                val parentStream = if (path.parent != null) Stream.of(path.parent) else Stream.empty()
                model.paths = Stream.concat(parentStream, Files.list(path)).collect(Collectors.toList())
            } else if (path.toFile().extension == "pdf") {
                println("desktop supported: ${Desktop.isDesktopSupported()}")
                desktop?.open(path.toFile())
            } else if (path.toFile().extension == "zip") {
                zipExit = path.parent
                val zipRoot = getFileSystem(path).getPath("/")
                val parentStream = if (path.parent != null) Stream.of(path.parent) else Stream.empty()
                val list = Stream.concat(parentStream, Files.list(zipRoot)).collect(Collectors.toList())
                println(list)
                model.paths = list
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
}

