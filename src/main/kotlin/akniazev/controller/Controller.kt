package akniazev.controller

import akniazev.common.*
import akniazev.common.View
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.VFS
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder
import java.awt.Desktop
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.nio.CharBuffer
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.time.ZoneId
import java.util.*
import java.util.zip.ZipError
import javax.imageio.ImageIO

typealias FtpFS = org.apache.commons.vfs2.FileSystem

/**
 * Contains all the logic of the application.
 *
 * Provides its instance of [View] with access to file system and FTP.
 * This implementation uses [kotlinx.coroutines] to perform background tasks,
 * [org.apache.commons.vfs2] to connect to FTP and [org.apache.tika.Tika] to improve file type recognition.
 *
 * @see View
 * @see kotlinx.coroutines
 * @see org.apache.commons.vfs2
 */
class ControllerImpl : Controller {

    override lateinit var view: View
    private var ftpConnected: Boolean = false
    private var ftpFileSystem: FtpFS? = null
    private val navigateBackStack: Deque<DisplayableFile> = EvictingStack(10)
    private val navigateForwardStack: Deque<DisplayableFile> = EvictingStack(10)
    private val buffer: CharBuffer = CharBuffer.allocate(150)
    private var currentFile: DisplayableFile? = null
    private val desktop: Desktop? = if (Desktop.isDesktopSupported()
            && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) Desktop.getDesktop() else null
    private var currentJob: Deferred<*>? = null


    /**
     * {@inheritDoc}
     * @see doNavigate
     */
    override fun navigate(file: DisplayableFile) {
        doNavigate(file) {
            navigateForwardStack.clear()
            navigateBackStack.push(it)
        }
    }

    /**
     * {@inheritDoc}
     * @see doNavigate
     */
    override fun navigateBack() {
        val newPath = navigateBackStack.pop()
        doNavigate(newPath, navigateForwardStack::push)
    }

    /**
     * {@inheritDoc}
     * @see doNavigate
     */
    override fun navigateForward() {
        val newPath = navigateForwardStack.pop()
        doNavigate(newPath, navigateBackStack::push)
    }

    /**
     * {@inheritDoc}
     * @see navigate
     * @see View.failWithMessage
     */
    override fun tryNavigate(pathText: String) {
        val normalized = pathText.replace('\\', '/')
        val path = Paths.get(normalized)
        if (Files.exists(path) && Files.isDirectory(path))
            navigate(SystemFile(path))
        else
            view.failWithMessage("Path should lead to a directory")
    }

    /**
     * {@inheritDoc}
     * Tries to open the file from new coroutine, which doesn't block the EDT.
     * @see java.awt.Desktop
     * @see java.awt.Desktop.open
     */
    override fun tryOpen(file: DisplayableFile) {
        if (file is SystemFile && desktop != null)
            GlobalScope.launch(Dispatchers.Default) {
                desktop.open(file.path.toFile())
            }
    }

    /**
     * {@inheritDoc}
     * @see readContent
     * @see View.previewText
     * @see View.failPreview
     */
    override fun readText(file: DisplayableFile) {
        GlobalScope.launch(Dispatchers.Swing) {
            currentJob?.cancel()
            val deferred = async(Dispatchers.Default) {
                try {
                    when (file) {
                        is SystemFile, is ZipFile -> {
                            val path = (file as? SystemFile)?.path ?: (file as? ZipFile)?.file
                            Right(readContent(Files.newBufferedReader(path)))
                        }
                        is FtpFile -> Right(readContent(BufferedReader(InputStreamReader(file.file.content.inputStream))))
                        else -> Left("Unexpected file type.")
                    }
                } catch(e: Exception) {
                    Left("Can't read contents of the file")
                }
            }
            currentJob = deferred
            val result = deferred.await()
            if (deferred.isCompleted)
                when(result) {
                    is Left -> view.failPreview(result.value)
                    is Right -> view.previewText(result.value)
                }
        }
    }

    /**
     * {@inheritDoc}
     * @see getScaledImage
     * @see View.previewImage
     * @see View.failPreview
     */
    override fun readImage(file: DisplayableFile) {
        GlobalScope.launch(Dispatchers.Swing) {
            currentJob?.cancel()
            val deferred = async(Dispatchers.Default) {
                try {
                    when (file) {
                        is SystemFile, is ZipFile -> {
                            val path = (file as? SystemFile)?.path ?: (file as? ZipFile)?.file
                            Right(getScaledImage(ImageIO.read(path?.toUri()?.toURL())))
                        }
                        is FtpFile -> Right(getScaledImage(ImageIO.read(file.file.content.inputStream)))
                        else -> Left("Unexpected file type.")
                    }
                } catch(e: Exception) {
                    Left("Can't preview image.")
                }
            }
            currentJob = deferred
            val result = deferred.await()
            if (deferred.isCompleted)
                when(result) {
                    is Left -> view.failPreview(result.value)
                    is Right -> view.previewImage(result.value)
                }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun cancelNavigation() {
        currentJob?.cancel()
    }

    /**
     * {@inheritDoc}
     * @see org.apache.commons.vfs2
     */
    override fun connectToFtp(host: String, port: String, user: String, pass: String) {
        GlobalScope.launch(Dispatchers.Swing) {
            view.showProgressBar()
            val builder = StringBuilder("ftp://")
            if (user.isNotEmpty()) {
                builder.append("$user:")
                if (pass != "") builder.append(pass)
                builder.append('@')
            }
            builder.append(host)
            if (port.isNotEmpty()) builder.append(":$port/")

            val url = builder.toString()
            val opts = FileSystemOptions()
            FtpFileSystemConfigBuilder.getInstance().setPassiveMode(opts, true)
            val deferred = async(Dispatchers.Default) {
                try {
                    Right(VFS.getManager().resolveFile(url, opts))
                } catch (e: Exception) {
                    Left("Can't connect to FTP with provided data.")
                }
            }
            currentJob = deferred
            val result = deferred.await()
            if (deferred.isCompleted) {
                when (result) {
                    is Left -> view.failWithMessage(result.value)
                    is Right -> {
                        ftpConnected = true
                        ftpFileSystem = result.value.fileSystem
                        navigate(FtpFile(result.value))
                        view.ftpConnected()
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see org.apache.commons.vfs2
     * @see View.ftpDisconnected
     */
    override fun disconnectFtp() {
        if (ftpConnected) {
            VFS.getManager().closeFileSystem(ftpFileSystem)
            navigateBackStack.clear()
            navigateForwardStack.clear()
            ftpConnected = false
        }
        view.ftpDisconnected()
    }

    /**
     * {@inheritDoc}
     */
    override fun cleanup() {
        if (ftpConnected) {
            VFS.getManager().closeFileSystem(ftpFileSystem)
            ftpConnected = false
        }
    }

    /**
     * Main navigation method.
     * Starts a coroutine, looking through a list of files the directory, represented by the given .
     * If the file refers to the zip archive, retrieves [java.nio.file.FileSystem] for it
     * and resolves current target to root of this FS.
     *
     * The status of the job is monitored by passing lambda with reference to [kotlinx.coroutines.Deferred.isActive]
     * to [listChildren], that walks the file tree. Thus, if the job is canceled by the user, the coroutine stops.
     *
     * If the job was completed successfully - invokes [View.updateFileList] with the result.
     * Otherwise invokes [View.failWithMessage].
     * [updatePosition] function takes care of updating the navigation stacks.
     *
     * @param file directory or zip file, target of the navigation
     * @param updatePosition function, updating states of the navigation stacks
     *
     * @see java.nio.file.FileSystem
     * @see kotlinx.coroutines
     * @see listChildren
     * @see View.updateFileList
     * @see View.failWithMessage
     */
    private fun doNavigate(file: DisplayableFile, updatePosition: (DisplayableFile) -> Unit) {
        GlobalScope.launch(Dispatchers.Swing) {
            currentJob?.cancel()
            val deferred = async(Dispatchers.Default) {
                try {
                    val target  = if (file is SystemFile && file.extension == "zip") {
                        val fileSystem = getFileSystem(file.path)
                        val newPath = fileSystem.getPath("/")
                        ZipFile(newPath, file.parent!!)
                    } else file
                    val children = listChildren(target, ::isActive)
                    Right(NavigationResult(file.parent, children, file.pathString, file is SystemFile))
                } catch (e: AccessDeniedException) {
                    Left("Operation is forbidden.")
                } catch (e: ZipError) {
                    Left("Can't open the archive.")
                } catch (e: Exception) {
                    Left("Something went wrong.")
                }
            }
            currentJob = deferred
            val result = deferred.await()
            if (deferred.isCompleted) {
                when (result) {
                    is Left -> view.failWithMessage(result.value)
                    is Right -> {
                        view.updateFileList(result.value.parent, result.value.children)
                        view.updateAddress(result.value.newPath, result.value.addressEnabled)
                        if (currentFile != null) {
                            updatePosition(currentFile!!)
                        }
                        currentFile = file
                        view.updateNavigation(navigateBackStack.isNotEmpty(),
                                navigateForwardStack.isNotEmpty(),
                                file is ZipFile || !file.isRoot)
                    }
                }
            }
        }
    }

    /**
     * Produces the list of all the directory contents.
     * Uses [isActive] param to monitor the state of the job and stops when it's no longer needed.
     *
     * @see DisplayableFile
     * @see java.nio.file.Files.walkFileTree
     * @see org.apache.commons.vfs2.FileObject
     */
    private fun listChildren(file: DisplayableFile, isActive: () -> Boolean): List<DisplayableFile> {
        val resultList = mutableListOf<DisplayableFile>()
        when(file) {
            is SystemFile -> {
                Files.walkFileTree(file.path, emptySet(), 1, object : SimpleFileVisitor<Path>() {
                    override fun visitFile(visitedFile: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (isActive()) {
                            resultList.add(SystemFile(visitedFile,
                                    attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()),
                                    if (visitedFile.toFile().isDirectory) null else attrs.size()))
                            return FileVisitResult.CONTINUE
                        }
                        return FileVisitResult.TERMINATE
                    }
                })
            }
            is ZipFile -> {
                Files.walkFileTree(file.file, emptySet(), 1, object : SimpleFileVisitor<Path>() {
                    override fun visitFile(visitedFile: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (isActive()) {
                            resultList.add(ZipFile(visitedFile, file.systemDir,
                                    attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()),
                                    if (Files.isDirectory(visitedFile)) null else attrs.size()))
                            return FileVisitResult.CONTINUE
                        }
                        return FileVisitResult.TERMINATE
                    }
                })
            }
            is FtpFile ->  {
                for (f in file.file.children) {
                    if (isActive()) {
                        resultList.add(FtpFile(f))
                    } else {
                        break
                    }
                }
            }
            else -> throw IllegalArgumentException()
        }
        return resultList
    }

    private fun getFileSystem(path: Path): FileSystem {
        val uri = URI.create("jar:file:///${path.toString().replace("\\", "/")}")
        return try {
            FileSystems.getFileSystem(uri)
        } catch (e: FileSystemNotFoundException) {
            FileSystems.newFileSystem(uri, mapOf("encoding" to "CP866"))
        }
    }

    private fun readContent(br: BufferedReader) = br.use { reader ->
        val read = reader.read(buffer)
        buffer.rewind()
        if (read > 0) buffer.substring(0, read) else ""
    }
}