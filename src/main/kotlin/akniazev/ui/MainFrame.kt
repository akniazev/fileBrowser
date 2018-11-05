package akniazev.ui

import akniazev.common.View
import akniazev.common.Controller
import akniazev.common.DisplayableFile
import akniazev.common.SystemFile
import java.awt.*
import java.awt.event.*
import java.awt.font.TextAttribute
import java.awt.image.BufferedImage
import java.nio.file.FileSystems
import java.time.ZonedDateTime
import javax.swing.*


class MainFrame(private val controller: Controller) : JFrame(), View {

    private val rightPanel = JPanel()
    private val centralPanel = JPanel()
    private val leftPanel = JPanel()
    private val topPanel = JPanel()

    private val model = TableModel()
    private val table = JTable(model)
    private val tableScroll = JScrollPane(table)

    private val extensionFilter = JComboBox<String>(arrayOf("All files", "txt", "png", "js", "json")).apply { font = Font("Arial", Font.PLAIN, 16) }
    private val connectFtpBtn = JButton("FTP")

    private val backBtn = JButton("Back")
    private val forwardBtn = JButton("Forward")
    private val upBtn = JButton("Up")
    private val addressBar = JTextField()


    private val previewNameLabel = JLabel().apply { font = Font("Arial", Font.ITALIC, 16) }
    private val previewModifiedLabel = JLabel().apply { font = Font("Arial", Font.ITALIC, 16) }
    private val previewSizeLabel = JLabel().apply { font = Font("Arial", Font.ITALIC, 16) }
    private val previewImageLabel = JLabel().apply { font = Font("Arial", Font.PLAIN, 16) }
    private val previewText = JTextArea().apply { font = Font("Arial", Font.PLAIN, 16) }
    private val previewContentLabel = JLabel().apply { font = Font("Arial", Font.PLAIN, 16) }
    private val contentPanel = JPanel()

    private val connectFtpDialog = ConnectFtpDialog(this, "Connect FTP", controller)

    init {
        controller.view = this

        layout = BorderLayout()
        size = Dimension(1000, 600)
        minimumSize = Dimension(1000, 600)
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        isVisible = true
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                controller.cleanup()
                dispose()
                System.exit(0)
            }
        })


        leftPanel.minimumSize = Dimension(170, leftPanel.height)
        leftPanel.preferredSize = Dimension(170, leftPanel.height)
        leftPanel.layout = BoxLayout(leftPanel, BoxLayout.Y_AXIS)
//        leftPanel.background = Color(123, 104, 156)
//        leftPanel.background = Color(71, 53, 77)
//        leftPanel.background = Color.BLACK
        leftPanel.background = Color.DARK_GRAY
        leftPanel.alignmentX = Component.LEFT_ALIGNMENT

        centralPanel.layout = BorderLayout()
        centralPanel.add(topPanel, BorderLayout.NORTH)
        centralPanel.add(tableScroll, BorderLayout.CENTER)


        rightPanel.minimumSize = Dimension(220, rightPanel.height)
        rightPanel.preferredSize = Dimension(220, rightPanel.height)
        rightPanel.layout = BoxLayout(rightPanel, BoxLayout.Y_AXIS)
        rightPanel.background = Color.WHITE

//        topPanel.minimumSize = Dimension(topPanel.width, 50)
        topPanel.preferredSize = Dimension(topPanel.width, 50)
        topPanel.alignmentY = Component.CENTER_ALIGNMENT
        topPanel.layout = FlowLayout(FlowLayout.LEFT)
        topPanel.background = Color(173, 174, 192)


        topPanel.add(backBtn)
        topPanel.add(forwardBtn)
        topPanel.add(upBtn)
        topPanel.add(JLabel("Current location: ").apply { font = Font("Arial", Font.PLAIN, 16) })
        topPanel.add(addressBar)

        add(leftPanel, BorderLayout.WEST)
        add(centralPanel, BorderLayout.CENTER)
        add(rightPanel, BorderLayout.EAST)

//        addressBar.maximumSize = Dimension(500, 30)
        addressBar.minimumSize = Dimension(230, 30)
        addressBar.preferredSize= Dimension(230, 30)



        rightPanel.border = BorderFactory.createEmptyBorder(10, 10, 0, 10)
        rightPanel.add(Box.createRigidArea(Dimension(0, 40)))

//        val f = Font("Arial", Font.PLAIN, 16)
//        val attrs = HashMap(f.attributes)
//        attrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON)
//        val f2 = f.deriveFont(attrs)

        rightPanel.add(JLabel("Name").apply { font = Font("Arial", Font.PLAIN, 16) })
        rightPanel.add(Box.createRigidArea(Dimension(0, 10)))
        rightPanel.add(previewNameLabel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))

        rightPanel.add(JLabel("Size").apply { font = Font("Arial", Font.PLAIN, 16) })
        rightPanel.add(Box.createRigidArea(Dimension(0, 10)))
        rightPanel.add(previewSizeLabel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))

        rightPanel.add(JLabel("Modified").apply { font = Font("Arial", Font.PLAIN, 16) })
        rightPanel.add(Box.createRigidArea(Dimension(0, 10)))
        rightPanel.add(previewModifiedLabel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))

        rightPanel.add(previewContentLabel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 10)))
        contentPanel.preferredSize = Dimension(200, 230)
        contentPanel.minimumSize = Dimension(200, 230)
        contentPanel.maximumSize = Dimension(200, 230)
        contentPanel.alignmentX = Component.LEFT_ALIGNMENT
        contentPanel.layout = BorderLayout(0, 0)
        contentPanel.background = rightPanel.background

        previewImageLabel.preferredSize = Dimension(200, 200)
        previewImageLabel.minimumSize = Dimension(200, 200)

        previewText.preferredSize = Dimension(200, 230)
        previewText.minimumSize = Dimension(200, 230)
        previewText.rows = 10
        previewText.background = rightPanel.background
        previewText.isEditable = false

        rightPanel.add(contentPanel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))

        table.columnModel.getColumn(0).cellRenderer = IconCellRenderer()
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)


//        table.columnModel.columnMargin = 5
        table.tableHeader.font = Font("Arial", Font.PLAIN, 16)
        table.font = Font("Arial", Font.PLAIN, 16)
        table.rowHeight = 30
        table.columnModel.getColumn(0).maxWidth = 35
        table.columnModel.getColumn(0).minWidth = 35
        table.columnModel.getColumn(1).minWidth = 150

        tableScroll.viewport.border = null
        tableScroll.viewportBorder = null
        tableScroll.border = null

//        table.autoCreateRowSorter = true
        table.showVerticalLines = false
        table.rowSelectionAllowed = true
        table.columnSelectionAllowed = false

        leftPanel.border = BorderFactory.createEmptyBorder(10, 20, 0, 20)
        val image = JLabel(ImageIcon(javaClass.getResource("/icons/JBsmall.png")))

        image.border = BorderFactory.createEmptyBorder(0, 0, 30, 0)
        leftPanel.add(image)
        leftPanel.add(JLabel("Filter files").apply { font = Font("Arial", Font.PLAIN, 16) })
        leftPanel.add(Box.createRigidArea(Dimension(0, 10)))


        extensionFilter.isEditable = true
        extensionFilter.alignmentX = Component.LEFT_ALIGNMENT
        extensionFilter.preferredSize = Dimension(130, 30)
        extensionFilter.maximumSize = Dimension(130, 30)
        extensionFilter.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                model.filterByExtension(extensionFilter.selectedItem as String)
            }
        }
        leftPanel.add(extensionFilter)
        leftPanel.add(Box.createRigidArea(Dimension(0, 20)))


        leftPanel.add(JLabel("Quick access").apply { font = Font("Arial", Font.PLAIN, 16) })
        leftPanel.add(Box.createRigidArea(Dimension(0, 10)))



        FileSystems.getDefault().rootDirectories.asSequence()
                .map {path ->
                    val button = JButton(path.toString())
                    button.maximumSize = Dimension(130, 30)
                    button.preferredSize = Dimension(130, 30)
                    button.background = Color.WHITE
//                    button.border = null
//                    button.isContentAreaFilled = false
//                    button.isBorderPainted = false
                    button.font = Font("Arial", Font.ITALIC, 16)
//                    button.alignmentX = Component.LEFT_ALIGNMENT
//                    button.icon = ImageIcon(javaClass.getResource("/icons/drive.png"))
                    button.addActionListener { controller.navigateTo(SystemFile(path)) }
                    button
                }.forEach {
                    leftPanel.add(it)
                    leftPanel.add(Box.createRigidArea(Dimension(0, 10)))
                }
//        leftPanel.add(Box.createRigidArea(Dimension(0, 10)))

//        leftPanel.add(JLabel("FTP").apply { font = Font("Arial", Font.PLAIN, 16) })
//        leftPanel.add(Box.createRigidArea(Dimension(0, 10)))

        connectFtpBtn.maximumSize = Dimension(130, 30)
        connectFtpBtn.preferredSize = Dimension(130, 30)
        connectFtpBtn.font = Font("Arial", Font.ITALIC, 16)
        connectFtpBtn.addActionListener { connectFtpDialog.isVisible = true }
        leftPanel.add(connectFtpBtn)




        // listeners

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = controller.handleTableClick(e.clickCount, model.files[table.selectedRow])
        })
        upBtn.addActionListener { controller.navigateTo(model.parentFile!!) }
        addressBar.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_ENTER) {
                    val normalizedPath = controller.tryNavigate(addressBar.text)
                    addressBar.text = normalizedPath
                }
            }
        })

        backBtn.addActionListener { controller.navigateBack() }
        forwardBtn.addActionListener { controller.navigateForward() }
    }

    override fun updateFileList(parent: DisplayableFile?, files: List<DisplayableFile>) {
        model.updateTable(parent, files)
    }

    override fun previewText(name: String, size: Long, lastModified: ZonedDateTime, textPreview: String) {
        previewNameLabel.text = name
        previewSizeLabel.text = size.toString()
        previewModifiedLabel.text = lastModified.toString()

        previewContentLabel.text = "Text"
        previewText.text = textPreview
        previewText.lineWrap = true

        contentPanel.removeAll()
        contentPanel.add(previewText)
        rightPanel.validate()
        rightPanel.updateUI()
    }

    override fun previewImage(name: String, size: Long, lastModified: ZonedDateTime, image: BufferedImage) {
        previewNameLabel.text = name
        previewSizeLabel.text = size.toString()
        previewModifiedLabel.text = lastModified.toString()

        previewContentLabel.text = "Image"
        previewImageLabel.icon = ImageIcon(image)

        contentPanel.removeAll()
        contentPanel.add(previewImageLabel, BorderLayout.CENTER)
        rightPanel.validate()
        rightPanel.updateUI()
    }
}