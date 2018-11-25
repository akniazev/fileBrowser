package akniazev.common

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.time.ZonedDateTime

interface View {
    fun updateFileList(parent: DisplayableFile?, files: List<DisplayableFile>)
    fun previewFile(name: String, lastModified: ZonedDateTime?, size: Long?)
    fun previewText(text: String)
    fun previewImage(image: BufferedImage)
    fun updateAddress(newAddress: String, enabled: Boolean)
    fun updateNavigation(backPossible: Boolean, forwardPossible: Boolean, upPossible: Boolean)
    fun failWithMessage(message: String)
    fun failPreview(message: String)

    fun ftpConnected()
    fun ftpDisconnected()
}

interface Controller {
    var view: View
    fun navigate(file: DisplayableFile)
    fun navigateBack()
    fun navigateForward()
    fun tryNavigate(pathText: String)
    fun tryOpen(file: DisplayableFile)
    fun readText(file: DisplayableFile)
    fun readImage(file: DisplayableFile)
    fun cancelNavigation()
    fun connectToFtp(host: String, port: String, user: String, pass: String)
    fun disconnectFtp()
    fun cleanup()
}


class NavigationResult(val parent: DisplayableFile?,
                       val children: List<DisplayableFile>,
                       val newPath: String,
                       val addressEnabled: Boolean)