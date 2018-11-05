package akniazev.ui

import akniazev.common.DisplayableFile
import akniazev.common.FileType
import akniazev.common.Controller
import akniazev.controller.createButton
import akniazev.controller.createLabel
import akniazev.controller.createTextField
import java.awt.*
import javax.swing.table.AbstractTableModel
import javax.swing.*
import javax.swing.table.TableCellRenderer


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


class ConnectFtpDialog(frame: Frame, title: String, controller: Controller) : JDialog(frame, title) {

    private val connectBtn = createButton("Connect")
    private val closeBtn = createButton("Close")

    private val host = createTextField(20)
    private val port = createTextField(5)
    private val user = createTextField(20)
    private val password = createTextField(20)


    init {
        size = Dimension(300, 300)
        setLocationRelativeTo(parent)

        layout = GridBagLayout()
        val constraints = GridBagConstraints()

        constraints.gridy = 0
        constraints.weightx = 1.toDouble()
        constraints.weighty = 1.toDouble()
        constraints.fill = GridBagConstraints.CENTER

        // First row
        constraints.gridx = 0
        add(createLabel("Host: "), constraints)
        constraints.gridx++
        add(host, constraints)

        // Second row
        constraints.gridy++
        constraints.gridx = 0
        add(createLabel("Port: "), constraints)
        constraints.gridx++
        add(port, constraints)

        // Third row
        constraints.gridy++
        constraints.gridx = 0
        add(createLabel("User: "), constraints)
        constraints.gridx++
        add(user, constraints)

        // Fourth row
        constraints.gridy++
        constraints.gridx = 0
        add(createLabel("Password: "), constraints)
        constraints.gridx++
        add(password, constraints)


        // Last row
        constraints.gridx = 0
        constraints.gridy++
        add(closeBtn, constraints)

        constraints.gridx++
        add(connectBtn, constraints)

        connectBtn.addActionListener {
            controller.connectToFtp(host.text, port.text, user.text, password.text)
            isVisible = false
        }
        closeBtn.addActionListener { isVisible = false }
    }

}

