package akniazev.common

import java.awt.image.BufferedImage
import java.time.ZonedDateTime

/**
 * Interface to be implemented by the main UI class of the application.
 *
 * @author akniazev
 */
interface View {
    /**
     * Updates the representation of the current directory on the view with the result from the [Controller].
     *
     * @param file single file from the current directory
     */
    fun updateFileList(file: DisplayableFile)

    /**
     * Displays selected file attributes.
     *
     * @param name name of the file
     * @param lastModified last time the file was modified
     * @param size size of the file in bytes or null if the file is a directory
     */
    fun previewFile(name: String, lastModified: ZonedDateTime?, size: Long?)

    /**
     * Displays preview of the text file.
     *
     * @param text file content
     */
    fun previewText(text: String)

    /**
     * Displays preview of the image file.
     *
     * @param image scaled image
     */
    fun previewImage(image: BufferedImage)

    /**
     * Updates content and state of the address bar.
     *
     * @param newAddress current location
     * @param enabled whether or not the address bar can be used in the current context
     */
    fun updateAddress(newAddress: String, enabled: Boolean)

    /**
     * Updates navigation buttons.
     *
     * @param backPossible true if navigation back is possible, false otherwise
     * @param forwardPossible true if navigation forward is possible, false otherwise
     * @param upPossible true if navigation up is possible, false otherwise
     */
    fun updateNavigation(backPossible: Boolean, forwardPossible: Boolean, upPossible: Boolean)

    /**
     * Notifies the user about a failed navigation attempt.
     *
     * @param message message for the user
     */
    fun failWithMessage(message: String)

    /**
     * Displays error message, indicating the failure of reading file contents.
     *
     * @param message message for the user
     */
    fun failPreview(message: String)

    /**
     * Displays the progress bar.
     */
    fun showProgressBar()

    /**
     * Callback method, notifying the veiw that ftp is successfully connected.
     */
    fun ftpConnected()

    /**
     * Callback method, notifying the veiw that ftp is disconnected.
     */
    fun ftpDisconnected()

    /**
     * Indicates that the navigation to a directory has started. Can be used to prepare the view.
     *
     * @param newParent new file, to be used as the target to navigate up
     */
    fun startNavigation(newParent: DisplayableFile?)

    /**
     * Indicates that the navigation to a directory has ended.
     */
    fun endNavigation()
}





/**
 * Interface to be implemented by the class, providing the logic for the application.
 *
 * @author akniazev
 */
interface Controller {

    /**
     * Instance of the [View], responsible for displaying results of the controller's methods.
     */
    var view: View

    /**
     * Main navigation entry point. Navigates to the directory, represented by the argument.
     * Should call [View.updateFileList] with the content of this directory.
     * Should call [View.updateNavigation] with the state of the navigation.
     * Should call [View.updateAddress] with the path to the directory.
     * Should call [View.failWithMessage] in case of error.
     *
     * @param file directory to navigate into
     */
    fun navigate(file: DisplayableFile)

    /**
     * Navigates to the previous directory, if there is one.
     * Should call [View.updateFileList] with the content of this directory.
     * Should call [View.updateNavigation] with the state of the navigation.
     * Should call [View.updateAddress] with the path to the directory.
     * Should call [View.failWithMessage] in case of error.
     */
    fun navigateBack()

    /**
     * Navigates to the directory, left by [Controller.navigateBack], if there is one.
     * Should call [View.updateFileList] with the content of this directory.
     * Should call [View.updateNavigation] with the state of the navigation.
     * Should call [View.updateAddress] with the path to the directory.
     * Should call [View.failWithMessage] in case of error.
     */
    fun navigateForward()

    /**
     * Tries to navigate to the drectory, provided by the user.
     * Should call [View.failWithMessage] if this directory doesn't exist.
     *
     * @param pathText path to the directory
     */
    fun tryNavigate(pathText: String)

    /**
     * Tries to open the file with it's default application, if this functionality is supported by the OS.
     *
     * @param file file to open
     */
    fun tryOpen(file: DisplayableFile)

    /**
     * Reads fixed amount of content from the text file.
     * Should call [View.previewText] with the content of the file.
     * Should call [View.failPreview] if the file can't be read.
     *
     * @param file text file to read
     */
    fun readText(file: DisplayableFile)

    /**
     * Reads the image and displays a scaled version of it.
     * Should call [View.previewImage] with the result.
     * Should call [View.failPreview] if the file can't be read.
     *
     * @param file image to display
     */
    fun readImage(file: DisplayableFile)

    /**
     * Cancels the current job, being processed by the controller.
     * Can be triggered by the user when the operation is no longer required or takes a lot of time.
     */
    fun cancelNavigation()

    /**
     * Connects to FTP with the specified data and credentials.
     * Should call [View.ftpConnected] and navigates to the root directory on success.
     * Should call [View.failWithMessage] if the connection fails.
     *
     * @param host address or ip of the host
     * @param port port number
     * @param user username or blank
     * @param pass or blank
     */
    fun connectToFtp(host: String, port: String, user: String, pass: String)

    /**
     * Closes current FTP connection and cleans up the resources.
     */
    fun disconnectFtp()

    /**
     * Maintenance method to be invoked on application closing.
     */
    fun cleanup()
}

