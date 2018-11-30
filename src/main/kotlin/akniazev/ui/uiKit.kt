package akniazev.ui

import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionEvent
import javax.swing.*

/**
 * This file contains constants and helper functions for building [MainFrame].
 */


val REGULAR_FONT = Font("Arial", Font.PLAIN, 16)
val TEXT_FIELD_DIMENSION = Dimension(130, 30)
val SMALL_VERTICAL_GAP = Dimension(0, 10)
val DEFAULT_EXTENSIONS = arrayOf("Show all files", "txt", "png", "jpg", "java", "kt", "js", "json")

fun createLabel(text: String = ""): JLabel {
    return JLabel(text).apply {
        font = REGULAR_FONT
    }
}

fun createTextField(columns: Int): JTextField {
    return JTextField(columns).apply {
        font = REGULAR_FONT
        minimumSize = TEXT_FIELD_DIMENSION
    }
}

fun createButton(text: String?): JButton {
    return JButton(text).apply {
        font = REGULAR_FONT
        maximumSize = Dimension(130, 30)
        preferredSize = Dimension(130, 30)
        background = Color.WHITE
    }
}

inline fun onAction(crossinline block: (ActionEvent) -> Unit): Action {
    return object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            block(e)
        }
    }
}