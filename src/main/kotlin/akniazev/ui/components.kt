package akniazev.ui

import akniazev.common.DisplayableFile
import akniazev.common.SystemFile
import com.sun.nio.zipfs.ZipPath
import java.awt.*
import java.nio.file.Path
import javax.swing.table.AbstractTableModel
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileSystemView
import javax.swing.table.TableCellRenderer
import kotlin.math.min


class TableModel : AbstractTableModel() {
    private val columns = listOf("", "Filename", "Extension", "Last Modified")
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
            0 -> file.extension
            1 -> file.name
            2 -> file.extension
            3 -> file.extension
            else -> throw IllegalArgumentException("Too many columns")
        }
    }

    fun updateTable(newParent: DisplayableFile?, newFiles: List<DisplayableFile>) {
        parentFile = newParent
        files = newFiles
        fireTableDataChanged()
    }
}

class IconCellRenderer : TableCellRenderer {
    private val label = JLabel().apply { isOpaque = true; horizontalAlignment = SwingConstants.CENTER }
    private val folderIcon = ImageIcon(javaClass.getResource("/icons/folder24.png"))
//    val view = FileSystemView.getFileSystemView()
    override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean,
                                               hasFocus: Boolean, row: Int, column: Int): Component {
        label.icon = folderIcon
//        label.icon = view.getSystemIcon(value as File) todo use as a backup maybe?
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


class ImagePreviewRenderer(var image: BufferedImage?) : JComponent() {
    override fun paintComponent(g: Graphics?) {
        if (image != null) {
            val g2 = g as Graphics2D
            val startX = if (width > image!!.width) (width - image!!.width) / 2 else 0
            val startY = if (height > image!!.height) (height - image!!.height) / 2 else 0
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2.drawImage(image, startX, startY, min(width, image!!.width), min(height, image!!.height), null)
        }
    }
}
