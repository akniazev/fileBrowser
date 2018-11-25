package akniazev.controller

import akniazev.common.*
import akniazev.ui.REGULAR_FONT
import akniazev.ui.TEXT_FIELD_DIMENSION
import org.apache.tika.Tika
import java.awt.Color
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.image.BufferedImage
import java.nio.file.Files
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

internal fun getScaledImage(img: BufferedImage): BufferedImage {
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

inline fun onAction(crossinline block: (ActionEvent) -> Unit): Action {
    return object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            block(e)
        }
    }
}

class EvictingStack<T>(private val limit: Int) : LinkedList<T>(){
    override fun push(e: T) {
        if (size == limit) removeLast()
        super.push(e)
    }
}

sealed class Either<out A, out B> {
    abstract fun <C> fold(left: (A) -> C, right: (B) -> C): C
}
class Left<out A>(val value: A) : Either<A, Nothing>() {
    override fun <C> fold(left: (A) -> C, right: (Nothing) -> C) = left(value)
}
class Right<out B>(val value: B) : Either<Nothing, B>() {
    override fun <C> fold(left: (Nothing) -> C, right: (B) -> C) = right(value)
}