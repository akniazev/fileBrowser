package akniazev.ui

import akniazev.controller.Controller
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTable
import kotlin.streams.asSequence


class MainFrame(private val controller: Controller) : JFrame() {

    private val model = TableModel()
    private val table = JTable(model)

    init {
        layout = BorderLayout()
        add(JScrollPane(table))
        model.paths = Files.list(Paths.get("C:/")).asSequence().toList()

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) = controller.handleTableClick(e)
        })

        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        size = Dimension(800, 600)
        isVisible = true
    }


}