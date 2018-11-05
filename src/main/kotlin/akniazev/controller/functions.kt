package akniazev.controller

import akniazev.common.REGULAR_FONT
import akniazev.common.TEXT_FIELD_DIMENSION
import java.awt.Color
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.nio.file.attribute.FileTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JTextField

fun FileTime.toSystemDateTime(): ZonedDateTime = this.toInstant().atZone(ZoneId.systemDefault())



fun getScaledImage(img: BufferedImage): BufferedImage {
    val result = BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB)
    val graphics = result.createGraphics()
    graphics.drawImage(img, 0, 0, 200, 200, null)
    graphics.dispose()
    return result
}


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
