package akniazev.common

import akniazev.ui.TableModel
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.time.ZonedDateTime

interface View {
    fun updateFileList(parent: DisplayableFile?, files: List<DisplayableFile>)
    fun previewText(name: String, size: Long, lastModified: ZonedDateTime, textPreview: String)
    fun previewImage(name: String, size: Long, lastModified: ZonedDateTime, image: BufferedImage)
}

interface Controller {
    var view: View
    fun handleTableClick(clickCount: Int, file: DisplayableFile)
    fun connectToFtp(host: String, port: String, user: String, pass: String)
    fun navigateTo(file: DisplayableFile)
    fun tryNavigate(pathText: String): String
    fun navigateBack()
    fun navigateForward()
    fun cleanup()
}