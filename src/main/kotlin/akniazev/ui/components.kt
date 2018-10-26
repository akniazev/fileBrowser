package akniazev.ui

import com.sun.nio.zipfs.ZipPath
import java.nio.file.Path
import javax.swing.table.AbstractTableModel



class TableModel : AbstractTableModel() {
    var paths: List<Path> = emptyList()
        set(newPaths) {
            field = newPaths
            fireTableDataChanged()
        }

    override fun getRowCount() = paths.size
    override fun getColumnCount() = 2
    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val path = paths[rowIndex]
        if (rowIndex == 0 && path.parent != null) return ".."
        return when(columnIndex) {
            0 -> if (path is ZipPath) path.toString() else path.toFile().name
            else -> if (path is ZipPath) "" else path.toFile().extension
        }
    }
}