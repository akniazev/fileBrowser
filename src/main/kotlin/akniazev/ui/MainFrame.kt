package akniazev.ui

import akniazev.controller.Controller
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
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

    private val extensionFilter = JComboBox<String>() // todo prefill
    private val connectFtpBtn = JButton("Connect")

    private val backBtn = JButton("Back")
    private val forwardBtn = JButton("Forward")
    private val upBtn = JButton("Up")
    private val addressBar = JTextField()


    private val previewNameLabel = JLabel()
    private val previewCreatedLabel = JLabel()
    private val previewAccessedLabel = JLabel()
    private val previewSizeLabel = JLabel()
    private val previewImageLabel = JLabel()
    private val previewText = JTextArea()
    private val previewContentLabel = JLabel()
    private val contentPanel = JPanel()

    init {
        controller.view = this

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



        rightPanel.border = BorderFactory.createEmptyBorder(10, 20, 0, 20)
        rightPanel.add(Box.createRigidArea(Dimension(0, 40)))

        rightPanel.add(JLabel("Name"))
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))
        rightPanel.add(previewNameLabel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))

        rightPanel.add(JLabel("Size"))
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))
        rightPanel.add(previewSizeLabel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))

        rightPanel.add(JLabel("Created"))
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))
        rightPanel.add(previewCreatedLabel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))

        rightPanel.add(JLabel("Accessed"))
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))
        rightPanel.add(previewAccessedLabel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))

        rightPanel.add(previewContentLabel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))
        contentPanel.preferredSize = Dimension(160, 160)
        contentPanel.minimumSize = Dimension(160, 160)
        contentPanel.maximumSize = Dimension(160, 160)
        contentPanel.alignmentX = Component.LEFT_ALIGNMENT

        previewImageLabel.preferredSize = Dimension(160, 160)
        previewImageLabel.minimumSize = Dimension(160, 160)

        previewText.preferredSize = Dimension(160, 160)
        previewText.minimumSize = Dimension(160, 160)

        rightPanel.add(contentPanel)
        rightPanel.add(Box.createRigidArea(Dimension(0, 20)))

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
            override fun mouseClicked(e: MouseEvent) = controller.handleTableClick(e)
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

    override fun previewText(name: String, created: ZonedDateTime, accessed: ZonedDateTime, size: Long, textPreview: String) {
        previewNameLabel.text = name
        previewCreatedLabel.text = created.toString()
        previewAccessedLabel.text = accessed.toString()
        previewSizeLabel.text = size.toString()
        previewContentLabel.text = "Text"
        previewText.text = textPreview
        previewText.lineWrap = true

        contentPanel.removeAll()
        contentPanel.add(previewText)
        rightPanel.validate()
        rightPanel.updateUI()
    }

    override fun previewImage(name: String, created: ZonedDateTime, accessed: ZonedDateTime, size: Long, image: BufferedImage) {
        previewNameLabel.text = name
        previewCreatedLabel.text = created.toString()
        previewAccessedLabel.text = accessed.toString()
        previewSizeLabel.text = size.toString()
        previewImageLabel.icon = ImageIcon(image)

        previewContentLabel.text = "Image"
        contentPanel.removeAll()
        contentPanel.add(previewImageLabel)
        rightPanel.validate()
        rightPanel.updateUI()
    }
}