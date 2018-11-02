package akniazev.ui

import akniazev.controller.Controller
import akniazev.controller.ControllerImpl
import akniazev.ui.IconCellRenderer
import akniazev.ui.TableModel
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellRenderer


class MainFrame(private val controller: Controller) : JFrame() {

    private val rightPanel = JPanel()
    private val centralPanel = JPanel()
    private val leftPanel = JPanel()
    private val topPanel = JPanel()

    private val model = TableModel()
    private val table = JTable(model)
    private val tableScroll = JScrollPane(table)

    private val extensionFilter = JComboBox<String>() // todo prefill
    private val connectFtpBtn = JButton("Connect")

    private val imagePanel = JPanel()

    private val backBtn = JButton("Back")
    private val forwardBtn = JButton("Forward")
    private val upBtn = JButton("Up")
    private val addressBar = JTextField()

    init {
        layout = BorderLayout()
        size = Dimension(1000, 600)
        minimumSize = Dimension(1000, 600)
        defaultCloseOperation = EXIT_ON_CLOSE
        isVisible = true


        leftPanel.minimumSize = Dimension(170, leftPanel.height)
        leftPanel.preferredSize = Dimension(170, leftPanel.height)
        leftPanel.layout = BoxLayout(leftPanel, BoxLayout.Y_AXIS)
        leftPanel.background = Color.CYAN


        centralPanel.layout = BorderLayout()
        centralPanel.add(topPanel, BorderLayout.NORTH)
        centralPanel.add(tableScroll, BorderLayout.CENTER)


        rightPanel.minimumSize = Dimension(200, rightPanel.height)
        rightPanel.preferredSize = Dimension(200, rightPanel.height)
        rightPanel.layout = BoxLayout(rightPanel, BoxLayout.Y_AXIS)
        rightPanel.background = Color.MAGENTA

        topPanel.minimumSize = Dimension(topPanel.width, 50)
        topPanel.layout = FlowLayout(FlowLayout.LEFT)
        topPanel.background = Color.GREEN


        topPanel.add(backBtn)
        topPanel.add(forwardBtn)
        topPanel.add(upBtn)
        topPanel.add(JLabel("Current location: "))
        topPanel.add(addressBar)

        add(leftPanel, BorderLayout.WEST)
        add(centralPanel, BorderLayout.CENTER)
        add(rightPanel, BorderLayout.EAST)

//        addressBar.maximumSize = Dimension(500, 30)
        addressBar.minimumSize = Dimension(230, 30)
        addressBar.preferredSize= Dimension(230, 30)


//        imagePanel.border = BorderFactory.createEmptyBorder()
        rightPanel.add(Box.createRigidArea(Dimension(0, 40)))
        imagePanel.preferredSize = Dimension(160, 160)
        imagePanel.minimumSize = Dimension(160, 160)
        imagePanel.maximumSize = Dimension(160, 160)
        rightPanel.add(imagePanel)





        table.columnModel.getColumn(0).cellRenderer = IconCellRenderer()
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)


//        table.columnModel.columnMargin = 5
        table.rowHeight = 30
        table.columnModel.getColumn(0).maxWidth = 35
        table.columnModel.getColumn(0).minWidth = 35
        table.columnModel.getColumn(1).minWidth = 150


//        table.autoCreateRowSorter = true
        table.showVerticalLines = false
        table.rowSelectionAllowed = true
        table.columnSelectionAllowed = false

        leftPanel.border = BorderFactory.createEmptyBorder(10, 20, 0, 20)
        val image = JLabel(ImageIcon(javaClass.getResource("/icons/JBsmall.png")))

        image.border = BorderFactory.createEmptyBorder(0, 0, 30, 0)
        leftPanel.add(image)
        leftPanel.add(JLabel("Filter files"))
        leftPanel.add(Box.createRigidArea(Dimension(0, 10)))


        extensionFilter.isEditable = true
        extensionFilter.alignmentX = Component.LEFT_ALIGNMENT
        extensionFilter.preferredSize = Dimension(130, 30)
        extensionFilter.maximumSize = Dimension(130, 30)
        leftPanel.add(extensionFilter)
        leftPanel.add(Box.createRigidArea(Dimension(0, 10)))


        leftPanel.add(JLabel("Quick access"))
        leftPanel.add(Box.createRigidArea(Dimension(0, 10)))



        FileSystems.getDefault().rootDirectories.asSequence()
                .map {path ->
                    val button = JButton(path.toString())
                    button.maximumSize = Dimension(130, 30)
                    button.preferredSize = Dimension(130, 30)
                    button.isContentAreaFilled = true
                    button.addActionListener { controller.navigateTo(path, model) }
                    button
                }.forEach {
                    leftPanel.add(it)
                    leftPanel.add(Box.createRigidArea(Dimension(0, 10)))
                }

        leftPanel.add(JLabel("FTP"))
        leftPanel.add(Box.createRigidArea(Dimension(0, 10)))

        connectFtpBtn.maximumSize = Dimension(130, 30)
        connectFtpBtn.preferredSize = Dimension(130, 30)
        leftPanel.add(connectFtpBtn)




        // listeners

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) = controller.handleTableClick(e)
        })
        upBtn.addActionListener { controller.toPreviousLevel(model) }
        addressBar.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_ENTER) {
                    val barText = addressBar.text.replace('\\', '/')
                    val path = Paths.get(barText)
                    if (Files.exists(path)) controller.navigateTo(path, model) // todo errorhandle
                    addressBar.text = barText
                }
            }
        })


    }
}