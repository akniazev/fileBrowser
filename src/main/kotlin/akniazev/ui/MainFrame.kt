package akniazev.ui

import akniazev.common.*
import akniazev.controller.createButton
import akniazev.controller.createLabel
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.nio.file.FileSystems
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*


class MainFrame(private val controller: Controller) : JFrame(), View {

    private val rightPanel = JPanel()
    private val centralPanel = JPanel()
    private val leftPanel = JPanel()
    private val topPanel = JPanel()

    private val model = TableModel()
    private val table = JTable(model)
    private val tableScroll = JScrollPane(table)

    private val extensionFilter = JComboBox<String>(arrayOf("Show all files", "txt", "png", "js", "json")).apply { font = REGULAR_FONT }
    private val connectFtpBtn = createButton("FTP")

    private val backBtn = createButton("Back")
    private val forwardBtn = createButton("Forward")
    private val upBtn = createButton("Up")
    private val addressBar = JTextField()


    private val previewNameLabel = JLabel().apply { font = Font("Arial", Font.BOLD, 16) }
    private val previewModifiedLabel = JLabel().apply { font = Font("Arial", Font.BOLD, 16) }
    private val previewSizeLabel = JLabel().apply { font = Font("Arial", Font.BOLD, 16) }
    private val previewText = JTextArea().apply { font = REGULAR_FONT }
    private val previewImageLabel = createLabel()
    private val previewContentLabel = createLabel()
    private val contentPanel = JPanel()

    private val connectFtpDialog = ConnectFtpDialog(this, "Connect FTP", controller)
    private var detailsPanelBuilt = false

    init {
        controller.view = this

        layout = BorderLayout()
        size = Dimension(1100, 600)
        minimumSize = Dimension(1100, 600)
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        isVisible = true
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                controller.cleanup()
                dispose()
                System.exit(0)
            }
        })

        buildLeftPanel()
        buildCentralPanel()
        buildTopPanel()
        prepareRightPanel()

        add(leftPanel, BorderLayout.WEST)
        add(centralPanel, BorderLayout.CENTER)
        add(rightPanel, BorderLayout.EAST)

        attachListeners()
        controller.navigateTo(SystemFile(FileSystems.getDefault().rootDirectories.asSequence().first()))
    }

    private fun attachListeners() {
        backBtn.addActionListener { controller.navigateBack() }
        forwardBtn.addActionListener { controller.navigateForward() }
        upBtn.addActionListener { controller.navigateTo(model.parentFile!!) }
        connectFtpBtn.addActionListener { connectFtpDialog.isVisible = true }

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = controller.handleTableClick(e.clickCount, model.files[table.selectedRow])
        })

        addressBar.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_ENTER) {
                    val normalizedPath = controller.tryNavigate(addressBar.text)
                    addressBar.text = normalizedPath
                }
            }
        })

        extensionFilter.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                model.filterByExtension(extensionFilter.selectedItem as String)
            }
        }
    }

    private fun buildLeftPanel(){
        leftPanel.minimumSize = Dimension(170, leftPanel.height)
        leftPanel.preferredSize = Dimension(170, leftPanel.height)
        leftPanel.layout = BoxLayout(leftPanel, BoxLayout.Y_AXIS)
        leftPanel.background = Color(71, 53, 77)
        leftPanel.alignmentX = Component.LEFT_ALIGNMENT

        leftPanel.border = BorderFactory.createEmptyBorder(10, 20, 0, 20)
        val image = JLabel(ImageIcon(javaClass.getResource("/icons/JBsmall.png")))

        image.border = BorderFactory.createEmptyBorder(0, 0, 30, 0)
        leftPanel.add(image)
        leftPanel.add(createLabel("Filter files"))
        leftPanel.add(Box.createRigidArea(Dimension(0, 10)))

        extensionFilter.isEditable = true
        extensionFilter.alignmentX = Component.LEFT_ALIGNMENT
        extensionFilter.preferredSize = Dimension(130, 30)
        extensionFilter.maximumSize = Dimension(130, 30)

        leftPanel.add(extensionFilter)
        leftPanel.add(Box.createRigidArea(Dimension(0, 20)))


        leftPanel.add(createLabel("Quick access"))
        leftPanel.add(Box.createRigidArea(Dimension(0, 10)))


        FileSystems.getDefault().rootDirectories.asSequence().forEach { path ->
            val button = createButton(path.toString())
            button.addActionListener { controller.navigateToRoot(path) }
            leftPanel.add(button)
            leftPanel.add(Box.createRigidArea(Dimension(0, 10)))
        }

        connectFtpBtn.maximumSize = Dimension(130, 30)
        connectFtpBtn.preferredSize = Dimension(130, 30)
        connectFtpBtn.font = Font("Arial", Font.ITALIC, 16)
        leftPanel.add(connectFtpBtn)

    }

    private fun prepareRightPanel() {
        rightPanel.minimumSize = Dimension(220, rightPanel.height)
        rightPanel.preferredSize = Dimension(220, rightPanel.height)
        rightPanel.layout = BoxLayout(rightPanel, BoxLayout.Y_AXIS)
        rightPanel.background = Color.WHITE
    }

    private fun buildRightPanel(){
        rightPanel.border = BorderFactory.createEmptyBorder(10, 10, 0, 10)
        rightPanel.add(Box.createRigidArea(Dimension(0, 45)))

        rightPanel.add(createLabel("Name"))
        rightPanel.add(Box.createRigidArea(Dimension(0, 10)))
        rightPanel.add(previewNameLabel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))

        rightPanel.add(createLabel("Size"))
        rightPanel.add(Box.createRigidArea(Dimension(0, 10)))
        rightPanel.add(previewSizeLabel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))

        rightPanel.add(createLabel("Last modified"))
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
    }

    private fun buildCentralPanel(){
        centralPanel.layout = BorderLayout()
        table.apply {
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            tableHeader.font = REGULAR_FONT
            font = REGULAR_FONT
            rowHeight = 30
            showVerticalLines = false
            rowSelectionAllowed = true
            columnSelectionAllowed = false
            columnModel.getColumn(0).minWidth = 35
            columnModel.getColumn(0).maxWidth = 35
            columnModel.getColumn(1).minWidth = 200
            columnModel.getColumn(1).preferredWidth = 500
            columnModel.getColumn(0).cellRenderer = IconCellRenderer()
        }

        tableScroll.viewport.border = null
        tableScroll.viewportBorder = null
        tableScroll.border = null

        centralPanel.add(topPanel, BorderLayout.NORTH)
        centralPanel.add(tableScroll, BorderLayout.CENTER)
    }

    private fun buildTopPanel() {
        topPanel.preferredSize = Dimension(topPanel.width, 50)
        topPanel.layout = BoxLayout(topPanel, BoxLayout.LINE_AXIS)
        topPanel.border = BorderFactory.createEmptyBorder(10, 35, 10, 10)
        topPanel.alignmentY = Component.CENTER_ALIGNMENT
        topPanel.background = Color(173, 174, 192)

        topPanel.add(backBtn)
        topPanel.add(Box.createRigidArea(Dimension(10, 0)))

        topPanel.add(forwardBtn)
        topPanel.add(Box.createRigidArea(Dimension(10, 0)))

        topPanel.add(upBtn)
        topPanel.add(Box.createRigidArea(Dimension(10, 0)))

        topPanel.add(createLabel("Current location: "))

        addressBar.minimumSize = Dimension(200, 30)
        addressBar.maximumSize = Dimension(1000, 30)
        addressBar.preferredSize= Dimension(200, 30)
        topPanel.add(addressBar)
    }

    override fun updateFileList(parent: DisplayableFile?, files: List<DisplayableFile>) {
        model.updateTable(parent, files)
    }

    override fun previewText(name: String, size: Long, lastModified: ZonedDateTime, textPreview: String) {
        if (!detailsPanelBuilt) {
            buildRightPanel()
            detailsPanelBuilt = true
        }
        previewNameLabel.text = name
        previewSizeLabel.text = size.toString() + " bytes"
        previewModifiedLabel.text = lastModified.format(DateTimeFormatter.ISO_DATE)

        previewContentLabel.text = "Text"
        previewText.text = textPreview
        previewText.lineWrap = true

        contentPanel.removeAll()
        contentPanel.add(previewText)
        rightPanel.validate()
        rightPanel.updateUI()
    }

    override fun previewImage(name: String, size: Long, lastModified: ZonedDateTime, image: BufferedImage) {
        if (!detailsPanelBuilt) {
            buildRightPanel()
            detailsPanelBuilt = true
        }
        previewNameLabel.text = name
        previewSizeLabel.text = size.toString() + " bytes"
        previewModifiedLabel.text = lastModified.format(DateTimeFormatter.ISO_DATE)

        previewContentLabel.text = "Image"
        previewImageLabel.icon = ImageIcon(image)

        contentPanel.removeAll()
        contentPanel.add(previewImageLabel, BorderLayout.CENTER)
        rightPanel.validate()
        rightPanel.updateUI()
    }

    override fun previewFile(name: String, size: Long, lastModified: ZonedDateTime) {
        if (!detailsPanelBuilt) {
            buildRightPanel()
            detailsPanelBuilt = true
        }
        previewNameLabel.text = name
        previewSizeLabel.text = size.toString() + " bytes"
        previewModifiedLabel.text = lastModified.format(DateTimeFormatter.ISO_DATE)

        previewContentLabel.text = ""
        contentPanel.removeAll()
        rightPanel.validate()
        rightPanel.updateUI()
    }
}