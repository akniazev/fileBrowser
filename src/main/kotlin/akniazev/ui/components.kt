package akniazev.ui

import akniazev.common.DisplayableFile
import com.sun.nio.zipfs.ZipPath
import java.nio.file.Path
import javax.swing.table.AbstractTableModel



class TableModel : AbstractTableModel() {
    private val columns = listOf("Filename", "Extension")
    var parentFile: DisplayableFile? = null
        private set
    var files: List<DisplayableFile> = emptyList()
        private set

    override fun getRowCount() = files.size
    override fun getColumnCount() = columns.size
    override fun getColumnName(col: Int): String = columns[col]
    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val file = files[rowIndex]
        return when(columnIndex) {
            0 -> file.name
            else -> file.extension
        }
    }

    fun updateTable(newParent: DisplayableFile?, newFiles: List<DisplayableFile>) {
        parentFile = newParent
        files = newFiles
        fireTableDataChanged()
    }
}