package akniazev.ui

import akniazev.common.*
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.nio.file.FileSystems
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import javax.swing.*


/**
 * Main UI class.
 *
 * Implements [View] interface to communicate with [Controller] instance.
 *
 * @param controller an instance of [Controller], that will serve as the sole provider of data
 * @author akniazev
 */
class MainFrame(private val controller: Controller) : JFrame(), View {

    private val rightPanel = JPanel()
    private val centralPanel = JPanel()
    private val leftPanel = JPanel()
    private val topPanel = JPanel()
    private val detailsPanel = JPanel()

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

    private val sizeHeader = createLabel("Size")
    private val spinner = JLabel()
    private val previewNameLabel = JLabel()
    private val previewModifiedLabel = JLabel()
    private val previewSizeLabel = JLabel()
    private val previewImageLabel = createLabel()
    private val previewText = JTextArea().apply { font = REGULAR_FONT; lineWrap = true }
    private val previewFailed = JTextArea().apply { font = REGULAR_FONT; lineWrap = true }
    private val previewContentLabel = createLabel()
    private val contentPanel = JPanel()

    private val connectFtpDialog = ConnectFtpDialog(this, "Connect to FTP", controller)
    private var detailsPanelBuilt = false

    private val connectFtpListener = ActionListener { connectFtpDialog.isVisible = true }
    private val disconnectFtpListener = ActionListener { controller.disconnectFtp() }

    init {
        controller.view = this
        title = "FileBrowser"
        iconImage = ImageIO.read(javaClass.getResource("/icons/JBsmall.png"))
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

        assignNames()
        buildLeftPanel()
        buildCentralPanel()
        buildTopPanel()
        prepareRightPanel()
        buildProgressDialog()

        add(leftPanel, BorderLayout.WEST)
        add(centralPanel, BorderLayout.CENTER)
        add(rightPanel, BorderLayout.EAST)

        attachListeners()
        onFileOpen(SystemFile(firstRoot))
        table.requestFocus()
    }

    /**
     * {@inheritDoc}
     */
    override fun updateFileList(file: DisplayableFile) {
        model.insertRow(file)
        spinner.isVisible = true
    }

    /**
     * {@inheritDoc}
     */
    override fun startNavigation(newParent: DisplayableFile?) {
        model.clearTable(newParent)
        spinner.isVisible = true
    }

    /**
     * {@inheritDoc}
     */
    override fun endNavigation() {
        spinner.isVisible = false
    }

    /**
     * {@inheritDoc}
     */
    override fun previewFile(name: String, lastModified: ZonedDateTime?,  size: Long?) {
        if (!detailsPanelBuilt) {
            buildRightPanel()
            detailsPanelBuilt = true
        }

        previewNameLabel.text = name
        previewModifiedLabel.text = lastModified?.format(DateTimeFormatter.ISO_DATE)

        if (size != null) {
            sizeHeader.text = "Size"
            previewSizeLabel.text = size.toString() + " bytes"
        } else {
            sizeHeader.text = ""
            previewSizeLabel.text = ""
        }

        previewContentLabel.text = ""
        contentPanel.removeAll()
        detailsPanel.validate()
        detailsPanel.updateUI()
    }

    /**
     * {@inheritDoc}
     */
    override fun previewText(text: String) {
        previewContentLabel.text = "Text"
        previewText.text = text

        contentPanel.removeAll()
        contentPanel.add(previewText)
        detailsPanel.validate()
        detailsPanel.updateUI()
    }

    /**
     * {@inheritDoc}
     */
    override fun previewImage(image: BufferedImage) {
        previewContentLabel.text = "Image"
        previewImageLabel.icon = ImageIcon(image)

        contentPanel.removeAll()
        contentPanel.add(previewImageLabel, BorderLayout.CENTER)
        detailsPanel.validate()
        detailsPanel.updateUI()
    }

    /**
     * {@inheritDoc}
     */
    override fun updateAddress(newAddress: String, enabled: Boolean) {
        addressBar.text = newAddress
        addressBar.isEnabled = enabled
    }

    /**
     * {@inheritDoc}
     */
    override fun updateNavigation(backPossible: Boolean, forwardPossible: Boolean, upPossible: Boolean) {
        backBtn.isEnabled = backPossible
        forwardBtn.isEnabled = forwardPossible
        upBtn.isEnabled = upPossible
        detailsPanel.updateUI()
    }

    /**
     * {@inheritDoc}
     */
    override fun failWithMessage(message: String) {
        progressDialog.isVisible = false
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE)
    }

    /**
     * {@inheritDoc}
     */
    override fun failPreview(message: String) {
        previewContentLabel.text = ""
        previewFailed.text = message

        contentPanel.removeAll()
        contentPanel.add(previewFailed, BorderLayout.CENTER)
        detailsPanel.validate()
        detailsPanel.updateUI()
    }

    /**
     * {@inheritDoc}
     */
    override fun ftpConnected() {
        progressDialog.isVisible = false
        connectFtpDialog.isVisible = false
        connectFtpBtn.text = "Disconnect"
        connectFtpBtn.removeActionListener(connectFtpListener)
        connectFtpBtn.addActionListener(disconnectFtpListener)
        connectFtpBtn.updateUI()
    }

    /**
     * {@inheritDoc}
     */
    override fun ftpDisconnected() {
        connectFtpBtn.text = "FTP"
        connectFtpBtn.removeActionListener(disconnectFtpListener)
        connectFtpBtn.addActionListener(connectFtpListener)
        connectFtpBtn.updateUI()
        onFileOpen(SystemFile(firstRoot))
    }

    /**
     * {@inheritDoc}
     */
    override fun showProgressBar() {
        progressDialog.isVisible = true
    }

    private fun assignNames() {
        addressBar.name = "addressBar"
        table.name = "table"
        upBtn.name = "upBtn"
        backBtn.name = "backBtn"
        forwardBtn.name = "forwardBtn"
        previewText.name = "textPreview"
        previewNameLabel.name = "previewNameLabel"
        connectFtpDialog.name = "connectFtpDialog"
        connectFtpBtn.name = "connectFtpBtn"
        previewImageLabel.name = "previewImage"
        extensionFilter.name = "extensionFilter"
    }

    private fun attachListeners() {
        backBtn.addActionListener {
            controller.navigateBack()
            table.requestFocus()
        }
        forwardBtn.addActionListener {
            controller.navigateForward()
            table.requestFocus()
        }
        upBtn.addActionListener {
            onFileOpen(model.parentFile!!)
            table.requestFocus()
        }
        connectFtpBtn.addActionListener(connectFtpListener)
        table.autoCreateRowSorter = true
        table.rowSorter.toggleSortOrder(1)
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
            put(KeyStroke.getKeyStroke("pressed TAB"), "selectNextComponent")
            put(KeyStroke.getKeyStroke("shift pressed TAB"), "selectPreviousComponent")
        }
        table.actionMap
        table.actionMap.apply {
            val nextRowAction = get("selectNextRow")
            val previousRowAction = get("selectPreviousRow")
            put("handleSelection", onAction {
                onFileOpen(model.files[table.convertRowIndexToModel(table.selectedRow)])
            })
            put("navigateUp", onAction {
                if (upBtn.isEnabled)
                    onFileOpen(model.parentFile!!)
            })
            put("navigateBack", onAction {
                if (backBtn.isEnabled){
                    controller.navigateBack()
                    table.requestFocus()
                }
            })
            put("navigateForward", onAction {
                if (forwardBtn.isEnabled) {
                    controller.navigateForward()
                    table.requestFocus()
                }
            })
            put("selectNextRow", onAction {
                nextRowAction.actionPerformed(it)
                showDetails(model.files[table.convertRowIndexToModel(table.selectedRow)])
            })
            put("selectPreviousRow", onAction {
                previousRowAction.actionPerformed(it)
                showDetails(model.files[table.convertRowIndexToModel(table.selectedRow)])
            })
            put("selectNextComponent", onAction {
                extensionFilter.requestFocus()
            })
            put("selectPreviousComponent", onAction {
                addressBar.requestFocus()
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
        val image = JLabel(ImageIcon(javaClass.getResource("/icons/JBsmall.png")))
        image.border = BorderFactory.createEmptyBorder(0, 0, 30, 0)
        extensionFilter.apply {
            isEditable = true
            alignmentX = Component.LEFT_ALIGNMENT
            preferredSize = Dimension(130, 30)
            maximumSize = Dimension(130, 30)
        }

        leftPanel.apply {
            minimumSize = Dimension(170, leftPanel.height)
            preferredSize = Dimension(170, leftPanel.height)
            layout = BoxLayout(leftPanel, BoxLayout.Y_AXIS)
            background = Color(71, 53, 77)
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createEmptyBorder(10, 20, 0, 20)

            add(image)
            add(Box.createRigidArea(Dimension(0, 40)))

            add(extensionFilter)
            add(Box.createRigidArea(Dimension(0, 40)))

            FileSystems.getDefault().rootDirectories.asSequence().forEach { path ->
                val button = createButton(path.toString())
                button.addActionListener { onFileOpen(SystemFile(path)) }
                leftPanel.add(button)
                leftPanel.add(Box.createRigidArea(SMALL_VERTICAL_GAP))
            }

            add(connectFtpBtn)
        }
    }

    private fun prepareRightPanel() {
        detailsPanel.apply {
            minimumSize = Dimension(220, detailsPanel.height)
            preferredSize = Dimension(220, detailsPanel.height)
            layout = BoxLayout(detailsPanel, BoxLayout.Y_AXIS)
            background = Color.WHITE
        }
        spinner.icon = ImageIcon(javaClass.getResource("/icons/spinner.gif"))
        spinner.horizontalAlignment = SwingConstants.CENTER
        val spinnerFooter = JPanel().apply {
            minimumSize = Dimension(220, 50)
            preferredSize = Dimension(220, 50   )
            layout = BorderLayout()
            background = Color.WHITE
            add(spinner)
        }
        rightPanel.apply {
            minimumSize = Dimension(220, this.height)
            preferredSize = Dimension(220, this.height)
            layout = BorderLayout(0, 0)
            background = Color.WHITE
        }
        rightPanel.add(detailsPanel, BorderLayout.CENTER)
        rightPanel.add(spinnerFooter, BorderLayout.SOUTH)
    }

    private fun buildRightPanel(){
        val boldFont = Font("Arial", Font.BOLD, 16)
        previewNameLabel.font = boldFont
        previewModifiedLabel.font = boldFont
        previewSizeLabel.font = boldFont
        val contentPanelSize = Dimension(200, 230)
        contentPanel.apply {
            preferredSize = contentPanelSize
            minimumSize = contentPanelSize
            maximumSize = contentPanelSize
            alignmentX = Component.LEFT_ALIGNMENT
            layout = BorderLayout(0, 0)
            background = detailsPanel.background
        }
        previewImageLabel.preferredSize = contentPanelSize
        previewImageLabel.minimumSize = contentPanelSize
        previewText.apply {
            preferredSize = contentPanelSize
            minimumSize = contentPanelSize
            rows = 10
            background = detailsPanel.background
            isEditable = false
        }
        detailsPanel.apply {
            border = BorderFactory.createEmptyBorder(10, 10, 0, 10)
            add(Box.createRigidArea(Dimension(0, 45)))

            add(createLabel("Name"))
            add(Box.createRigidArea(SMALL_VERTICAL_GAP))
            add(previewNameLabel)
            add(Box.createRigidArea(Dimension(0, 20)))

            add(createLabel("Last modified"))
            add(Box.createRigidArea(SMALL_VERTICAL_GAP))
            add(previewModifiedLabel)
            add(Box.createRigidArea(Dimension(0, 20)))

            add(sizeHeader)
            add(Box.createRigidArea(SMALL_VERTICAL_GAP))
            add(previewSizeLabel)
            add(Box.createRigidArea(Dimension(0, 20)))

            add(previewContentLabel)
            add(Box.createRigidArea(SMALL_VERTICAL_GAP))

            add(contentPanel)
        }
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
        addressBar.apply {
            minimumSize = Dimension(200, 30)
            maximumSize = Dimension(1000, 30)
            preferredSize= Dimension(200, 30)
        }
        topPanel.apply {
            preferredSize = Dimension(topPanel.width, 50)
            layout = BoxLayout(topPanel, BoxLayout.LINE_AXIS)
            border = BorderFactory.createEmptyBorder(10, 35, 10, 10)
            alignmentY = Component.CENTER_ALIGNMENT
            background = Color(173, 174, 192)

            add(backBtn)
            add(Box.createRigidArea(Dimension(10, 0)))
            add(forwardBtn)
            add(Box.createRigidArea(Dimension(10, 0)))
            add(upBtn)
            add(Box.createRigidArea(Dimension(10, 0)))
            add(createLabel("Current location: "))
            add(addressBar)
        }

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

    private fun onFileOpen(file: DisplayableFile) {
        if (file.navigable) {
            controller.navigate(file)
        } else {
            controller.tryOpen(file)
        }
    }

    private fun showDetails(file: DisplayableFile) {
        previewFile(file.name, file.lastModified, file.size)
        if (file.type == FileType.TEXT) {
            previewContentLabel.text = "Loading content.."
            controller.readText(file)
        } else if (file.type == FileType.IMAGE) {
            previewContentLabel.text = "Loading content.."
            controller.readImage(file)
        }
    }

    private val firstRoot: Path
        get() = FileSystems.getDefault().rootDirectories.asSequence().first()
}