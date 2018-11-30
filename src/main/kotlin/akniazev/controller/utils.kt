package akniazev.controller

import akniazev.common.*
import org.apache.tika.Tika
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.util.*


/**
 * Object that assigns [FileType] instance to each [DisplayableFile].
 * Image types are the ones supported by [javax.imageio.ImageIO].
 * For local and zip files it uses [org.apache.tika.Tika] to improve detection of text files.
 */
object FileTypeDetector {

    private val tika = Tika()
    private val text = setOf("text", "javascript", "json", "xml", "htlm")
    private val image = setOf("png", "jpeg", "jpg", "bmp", "gif")

    /**
     * Detection method.
     *
     * @param file displayable file
     * @return [FileType] enum instance, assigned to that file.
     */
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


/**
 * Container class with information about current navigation target.
 *
 * @param parent previous directory in the hierarchy
 * @param children contents of the directory
 * @param newPath path to the current directory
 * @param addressEnabled whether navigation from address bar is possible
 */
class NavigationResult(val parent: DisplayableFile?,
                       val children: List<DisplayableFile>,
                       val newPath: String,
                       val addressEnabled: Boolean)

/**
 * Subclass of [LinkedList], with maximum capacity. If the capacity is reached, the last element is removed.
 *
 * @param limit capacity of thie stack
 */
class EvictingStack<T>(private val limit: Int) : LinkedList<T>(){
    override fun push(e: T) {
        if (size == limit) removeLast()
        super.push(e)
    }
}


/**
 * Class to represent the result of an operation
 * where [Left] instance indicates failure and [Right] instance indicates success.
 */
sealed class Either<out A, out B> {
    abstract fun <C> fold(left: (A) -> C, right: (B) -> C): C
}
class Left<out A>(val value: A) : Either<A, Nothing>() {
    override fun <C> fold(left: (A) -> C, right: (Nothing) -> C) = left(value)
}
class Right<out B>(val value: B) : Either<Nothing, B>() {
    override fun <C> fold(left: (Nothing) -> C, right: (B) -> C) = right(value)
}


/**
 * Function to scale the image to fit certain size.
 *
 * @param img image to scale
 * @return scaled image
 */
internal fun getScaledImage(img: BufferedImage): BufferedImage {
    val result = BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB)
    val graphics = result.createGraphics()
    graphics.drawImage(img, 0, 0, 200, 200, null)
    graphics.dispose()
    return result
}