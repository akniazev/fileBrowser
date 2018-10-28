package akniazev.ui

import akniazev.controller.Controller
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.*
import javax.swing.*


class MainFrame(private val controller: Controller) : JFrame() {

    private val mainPanel = JPanel()
    private val model = TableModel()
    private val table = JTable(model)
    private val toolbar = JToolBar()
    private val rootSelector = JComboBox<Path>(FileSystems.getDefault().rootDirectories.asSequence().toList().toTypedArray())
    private val upButton = JButton("Go up")
    private val connectFtpButton = JButton("Connect")

    init {
        layout = BorderLayout()

        toolbar.layout = FlowLayout(FlowLayout.LEFT)
        toolbar.add(upButton)
        toolbar.add(connectFtpButton)
        toolbar.add(rootSelector)

        mainPanel.layout = BorderLayout()
        mainPanel.add(toolbar, BorderLayout.NORTH)
        mainPanel.add(JScrollPane(table), BorderLayout.CENTER)
        add(mainPanel)

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) = controller.handleTableClick(e)
        })
        upButton.addActionListener { controller.toPreviousLevel(model) }
        connectFtpButton.addActionListener { controller.connectToFtp("localhost", "58562", "user", "password", model) }
        rootSelector.addActionListener { controller.changeRoot(rootSelector.selectedItem as Path, model) }
        rootSelector.selectedIndex = 0

        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        size = Dimension(800, 600)
        isVisible = true
    }

}