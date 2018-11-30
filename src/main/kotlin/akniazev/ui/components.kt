package akniazev.ui

import akniazev.common.DisplayableFile
import akniazev.common.FileType
import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.table.AbstractTableModel
import javax.swing.*
import javax.swing.table.TableCellRenderer


/**
 * This file contains components, used by the [MainFrame] and [ConnectFtpDialog].
 */


class TableModel : AbstractTableModel() {
    private val columns = listOf("", "Filename", "Extension")
    private var filter: String = "Show all files"
    private var cachedFiles: List<DisplayableFile> = emptyList()
    var files: List<DisplayableFile> = emptyList()
        private set
    var parentFile: DisplayableFile? = null
        private set

    override fun getRowCount() = files.size
    override fun getColumnCount() = columns.size
    override fun getColumnName(col: Int): String = columns[col]
    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val file = files[rowIndex]
        return when(columnIndex) {
            0 -> file.type
            1 -> file.name
            2 -> file.extension
            else -> throw IllegalArgumentException("Too many columns")
        }
    }

    fun filterByExtension(ext: String) {
        filter = ext
        files = doFilter(filter, cachedFiles)
        fireTableDataChanged()
    }

    fun updateTable(newParent: DisplayableFile?, newFiles: List<DisplayableFile>) {
        cachedFiles = newFiles
        files = doFilter(filter, cachedFiles)
        parentFile = newParent
        fireTableDataChanged()
    }

    private fun doFilter(ext: String, allFiles: List<DisplayableFile>): List<DisplayableFile> {
        return if (ext == "Show all files") allFiles
               else allFiles.asSequence()
                            .filter { it.isDirectory || it.extension == filter }
                            .toList()
    }
}



class IconCellRenderer : TableCellRenderer {
    private val label = JLabel().apply { isOpaque = true; horizontalAlignment = SwingConstants.CENTER }
    private val folderIcon = ImageIcon(javaClass.getResource("/icons/folder24.png"))
    private val textIcon = ImageIcon(javaClass.getResource("/icons/text.png"))
    private val imageIcon = ImageIcon(javaClass.getResource("/icons/image.png"))
    private val unknownIcon = ImageIcon(javaClass.getResource("/icons/unknown.png"))

    override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean,
                                               hasFocus: Boolean, row: Int, column: Int): Component {
        label.icon = when(value) {
            FileType.DIRECTORY -> folderIcon
            FileType.TEXT -> textIcon
            FileType.IMAGE -> imageIcon
            else -> unknownIcon
        }
        if (isSelected) {
            label.background = table?.selectionBackground ?: label.background
            label.foreground = table?.selectionForeground ?: label.foreground
        } else {
            label.background = table?.background ?: label.background
            label.foreground = table?.foreground ?: label.foreground
        }
        return label
    }
}


class Validator<T : JComponent>(private val component: T, var valid: Boolean = false, val validationFun: (T) -> Boolean){
    private val validationSuccessBorder = BorderFactory.createLineBorder(Color.GREEN, 1)
    private val validationFailBorder = BorderFactory.createLineBorder(Color.RED, 1)

    fun validate() : Boolean{
        valid = validationFun(component)
        if (valid) {
            component.border = validationSuccessBorder
        } else {
            component.border = validationFailBorder
        }
        return valid
    }
}

class PlaceholderListener(private val placeholder: String) : FocusAdapter() {
    override fun focusGained(e: FocusEvent) {
        val target = e.source as JTextField
        if (target.text == placeholder) {
            target.text = ""
            target.foreground = Color.BLACK
        }
    }

    override fun focusLost(e: FocusEvent) {
        val target = e.source as JTextField
        if (target.text.isEmpty()) {
            target.foreground = Color.GRAY
            target.text = placeholder
        }
    }
}


