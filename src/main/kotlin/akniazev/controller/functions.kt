package akniazev.controller

import java.awt.image.BufferedImage
import java.nio.file.attribute.FileTime
import java.time.ZoneId
import java.time.ZonedDateTime

fun FileTime.toSystemDateTime(): ZonedDateTime = this.toInstant().atZone(ZoneId.systemDefault())



fun getScaledImage(img: BufferedImage): BufferedImage {
    val result = BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB)
    val graphics = result.createGraphics()
    graphics.drawImage(img, 0, 0, 200, 200, null)
    graphics.dispose()
    return result
}

