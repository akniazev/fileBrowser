package akniazev

import akniazev.controller.ControllerImpl
import akniazev.ui.MainFrame

import javax.swing.SwingUtilities

fun main(args: Array<String>) {
    SwingUtilities.invokeLater { MainFrame(ControllerImpl()) }
}
