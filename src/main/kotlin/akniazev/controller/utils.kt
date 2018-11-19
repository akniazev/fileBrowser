package akniazev.controller

import akniazev.common.*
import org.apache.tika.Tika
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.lang.Exception
import java.nio.file.Files
import java.security.PrivilegedActionException
import java.util.*
import javax.swing.*

object FileTypeDetector {
    private val tika = Tika()
    private val text = setOf("text", "javascript", "json", "xml", "htlm")
    private val image = setOf("png", "jpeg", "jpg", "bmp", "gif")

    fun detect(file: DisplayableFile): FileType {
        if (file.isDirectory) return FileType.DIRECTORY
        if (image.contains(file.extension.toLowerCase())) return FileType.IMAGE
        return when(file) {
            is SystemFile, is ZipFile -> {
                val path = (file as? SystemFile)?.path ?: (file as? ZipFile)?.file
                if (Files.isReadable(path)) {
                    val type = tika.detect(path)
                    when {
                        text.asSequence().any { type.contains(it.toLowerCase()) } -> FileType.TEXT
                        else -> FileType.UNKNOWN
                    }
                } else FileType.UNKNOWN
            }
            is FtpFile -> {
                val contentType = file.file.content.contentInfo.contentType
                if (text.asSequence().any { contentType.contains(it.toLowerCase()) }) FileType.TEXT
                else FileType.UNKNOWN
            }
            else -> throw IllegalArgumentException()
        }
    }
}

inline fun notifyOnError(parent: Component, message: String = "Unable to perform the operation.", block: () -> Unit) {
    try {
        block()
    } catch (e: PrivilegedActionException) {
        JOptionPane.showMessageDialog(parent, "Access denied.", "Error", JOptionPane.ERROR_MESSAGE)
    }  catch (e: IncorrectPathException) {
        JOptionPane.showMessageDialog(parent, "Path should lead to a directory", "Error", JOptionPane.ERROR_MESSAGE)
    } catch (e: Exception) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE)
    }
}

class IncorrectPathException : RuntimeException()

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

class EvictingStack<T>(private val limit: Int) : LinkedList<T>(){
    override fun push(e: T) {
        if (size == limit) removeLast()
        super.push(e)
    }
}
