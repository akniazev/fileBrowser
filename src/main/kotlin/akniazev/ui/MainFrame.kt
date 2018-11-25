package akniazev.ui

import akniazev.common.*
import akniazev.controller.*
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

    private val extensionFilter = JComboBox<String>(DEFAULT_EXTENSIONS).apply { font = REGULAR_FONT }
    private val connectFtpBtn = createButton("FTP")

    private val backBtn = createButton("Back")
    private val forwardBtn = createButton("Forward")
    private val upBtn = createButton("Up")
    private val addressBar = JTextField()

    private val progressDialog = JDialog(this, "Loading", false)

    private val previewNameLabel = JLabel().apply { font = Font("Arial", Font.BOLD, 16) }
    private val previewModifiedLabel = JLabel().apply { font = Font("Arial", Font.BOLD, 16) }
    private val previewSizeLabel = JLabel().apply { font = Font("Arial", Font.BOLD, 16) }
    private val previewImageLabel = createLabel()
    private val previewText = JTextArea().apply { font = REGULAR_FONT; lineWrap = true }
    private val previewFailed = JTextArea().apply { font = REGULAR_FONT; lineWrap = true }
    private val previewContentLabel = createLabel()
    private val contentPanel = JPanel()

    private val connectFtpDialog = ConnectFtpDialog(this, "Connect FTP", controller)
    private var detailsPanelBuilt = false

    private val connectFtpListener = ActionListener { connectFtpDialog.isVisible = true }
    private val disconnectFtpListener = ActionListener { controller.disconnectFtp() }

    init {
        controller.view = this

        layout = BorderLayout()
        size = Dimension(1100, 600)
        minimumSize = Dimension(1100, 600)
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        isVisible = true
        setLocationRelativeTo(null)
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
        buildProgressDialog()

        add(leftPanel, BorderLayout.WEST)
        add(centralPanel, BorderLayout.CENTER)
        add(rightPanel, BorderLayout.EAST)

        attachListeners()
        onFileOpen(SystemFile(firstRoot()))
    }

    private fun attachListeners() {
        backBtn.addActionListener {
            progressDialog.isVisible = true
            controller.navigateBack()
        }
        forwardBtn.addActionListener {
            progressDialog.isVisible = true
            controller.navigateForward()

        }
        upBtn.addActionListener {
            progressDialog.isVisible = true
            onFileOpen(model.parentFile!!)
        }
        connectFtpBtn.addActionListener(connectFtpListener)
        table.autoCreateRowSorter = true
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val file = model.files[table.convertRowIndexToModel(table.selectedRow)]
                if (e.clickCount == 2) {
                    onFileOpen(file)
                } else {
                    showDetails(file)
                }
            }
        })

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).apply {
            put(KeyStroke.getKeyStroke("pressed ENTER"), "handleSelection")
            put(KeyStroke.getKeyStroke("alt pressed UP"), "navigateUp")
            put(KeyStroke.getKeyStroke("alt pressed RIGHT"), "navigateForward")
            put(KeyStroke.getKeyStroke("alt pressed LEFT"), "navigateBack")
            put(KeyStroke.getKeyStroke("pressed BACK_SPACE"), "navigateBack")
        }
        table.actionMap.apply {
            put("handleSelection", onAction {
                onFileOpen(model.files[table.convertRowIndexToModel(table.selectedRow)])
            })
            put("navigateUp", onAction {
                if (upBtn.isEnabled)
                    onFileOpen(model.parentFile!!)
            })
            put("navigateBack", onAction {
                if (backBtn.isEnabled){
                    progressDialog.isVisible = true
                    controller.navigateBack()
                }
            })
            put("navigateForward", onAction {
                if (forwardBtn.isEnabled) {
                    progressDialog.isVisible = true
                    controller.navigateForward()
                }
            })
            val nextRowAction = get("selectNextRow")
            put("selectNextRow", onAction {
                nextRowAction.actionPerformed(it)
                showDetails(model.files[table.convertRowIndexToModel(table.selectedRow)])
            })
            val previousRowAction = get("selectPreviousRow")
            put("selectPreviousRow", onAction {
                previousRowAction.actionPerformed(it)
                showDetails(model.files[table.convertRowIndexToModel(table.selectedRow)])
            })
        }

        addressBar.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_ENTER) {
                    controller.tryNavigate(addressBar.text)
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
        leftPanel.add(Box.createRigidArea(Dimension(0, 40)))

        extensionFilter.isEditable = true
        extensionFilter.alignmentX = Component.LEFT_ALIGNMENT
        extensionFilter.preferredSize = Dimension(130, 30)
        extensionFilter.maximumSize = Dimension(130, 30)

        leftPanel.add(extensionFilter)
        leftPanel.add(Box.createRigidArea(Dimension(0, 40)))

        FileSystems.getDefault().rootDirectories.asSequence().forEach { path ->
            val button = createButton(path.toString())
            button.addActionListener { onFileOpen(SystemFile(path)) }
            leftPanel.add(button)
            leftPanel.add(Box.createRigidArea(SMALL_VERTICAL_GAP))
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
        rightPanel.add(Box.createRigidArea(SMALL_VERTICAL_GAP))
        rightPanel.add(previewNameLabel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))

        rightPanel.add(createLabel("Last modified"))
        rightPanel.add(Box.createRigidArea(SMALL_VERTICAL_GAP))
        rightPanel.add(previewModifiedLabel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))

        rightPanel.add(createLabel("Size"))
        rightPanel.add(Box.createRigidArea(SMALL_VERTICAL_GAP))
        rightPanel.add(previewSizeLabel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))

        rightPanel.add(previewContentLabel)
        rightPanel.add(Box.createRigidArea(SMALL_VERTICAL_GAP))

        val contentPanelSize = Dimension(200, 230)
        contentPanel.preferredSize = contentPanelSize
        contentPanel.minimumSize = contentPanelSize
        contentPanel.maximumSize = contentPanelSize
        contentPanel.alignmentX = Component.LEFT_ALIGNMENT
        contentPanel.layout = BorderLayout(0, 0)
        contentPanel.background = rightPanel.background

        previewImageLabel.preferredSize = contentPanelSize
        previewImageLabel.minimumSize = contentPanelSize

        previewText.preferredSize = contentPanelSize
        previewText.minimumSize = contentPanelSize
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

    private fun buildProgressDialog() {
        val btn = createButton("Cancel")
        btn.addActionListener { controller.cancelNavigation(); progressDialog.isVisible = false }
        val navigationProgressBar = JProgressBar().apply {
            isIndeterminate = true
            preferredSize = Dimension(300, 30)
            foreground = Color(71, 53, 77)
        }
        progressDialog.apply {
            size = Dimension(470, 90)
            isAlwaysOnTop = true
            layout = FlowLayout(FlowLayout.LEFT)
            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) {
                    super.windowClosing(e)
                    controller.cancelNavigation()
                }
            })
            add(navigationProgressBar)
            add(btn)
            pack()
            setLocationRelativeTo(this)
        }
    }

    override fun updateFileList(parent: DisplayableFile?, files: List<DisplayableFile>) {
        model.updateTable(parent, files)
        progressDialog.isVisible = false
    }

    override fun previewFile(name: String, lastModified: ZonedDateTime?,  size: Long?) {
        if (!detailsPanelBuilt) {
            buildRightPanel()
            detailsPanelBuilt = true
        }
        previewNameLabel.text = name
        previewModifiedLabel.text = lastModified?.format(DateTimeFormatter.ISO_DATE)
        previewSizeLabel.text = size.toString() + " bytes"

        previewContentLabel.text = ""
        contentPanel.removeAll()
        rightPanel.validate()
        rightPanel.updateUI()
    }

    override fun previewText(text: String) {
        previewContentLabel.text = "Text"
        previewText.text = text

        contentPanel.removeAll()
        contentPanel.add(previewText)
        rightPanel.validate()
        rightPanel.updateUI()
    }

    override fun previewImage(image: BufferedImage) {
        previewContentLabel.text = "Image"
        previewImageLabel.icon = ImageIcon(image)

        contentPanel.removeAll()
        contentPanel.add(previewImageLabel, BorderLayout.CENTER)
        rightPanel.validate()
        rightPanel.updateUI()
    }

    override fun updateAddress(newAddress: String, enabled: Boolean) {
        addressBar.text = newAddress
        addressBar.isEnabled = enabled
    }

    override fun updateNavigation(backPossible: Boolean, forwardPossible: Boolean, upPossible: Boolean) {
        backBtn.isEnabled = backPossible
        forwardBtn.isEnabled = forwardPossible
        upBtn.isEnabled = upPossible
        rightPanel.updateUI()
    }

    override fun failWithMessage(message: String) {
        progressDialog.isVisible = false
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE)
    }

    override fun failPreview(message: String) {
        progressDialog.isVisible = false
        previewContentLabel.text = ""
        previewFailed.text = message

        contentPanel.removeAll()
        contentPanel.add(previewFailed, BorderLayout.CENTER)
        rightPanel.validate()
        rightPanel.updateUI()
    }

    override fun ftpConnected() {
        connectFtpBtn.text = "Disconnect"
        connectFtpBtn.removeActionListener(connectFtpListener)
        connectFtpBtn.addActionListener(disconnectFtpListener)
        connectFtpBtn.updateUI()
    }

    override fun ftpDisconnected() {
        connectFtpBtn.text = "FTP"
        connectFtpBtn.removeActionListener(disconnectFtpListener)
        connectFtpBtn.addActionListener(connectFtpListener)
        connectFtpBtn.updateUI()
        onFileOpen(SystemFile(firstRoot()))
    }

    private fun onFileOpen(file: DisplayableFile) {
        if (file.navigable) {
            progressDialog.isVisible = true
            controller.navigate(file)
        } else {
            controller.tryOpen(file)
        }
    }

    private fun showDetails(file: DisplayableFile) {
        previewFile(file.name, file.lastModified, file.size)
        if (file.type == FileType.TEXT) {
            controller.readText(file)
        } else if (file.type == FileType.IMAGE) {
            controller.readImage(file)
        }
    }

    private fun firstRoot() = FileSystems.getDefault().rootDirectories.asSequence().first()
}