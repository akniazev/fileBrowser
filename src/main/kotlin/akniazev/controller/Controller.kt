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


    override fun navigate(file: DisplayableFile) {
        doNavigate(file) {
            navigateForwardStack.clear()
            navigateBackStack.push(it)
        }
    }

    private inline fun doNavigate(file: DisplayableFile, crossinline updatePosition: (DisplayableFile) -> Unit) {
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

    override fun navigateBack() {
        val newPath = navigateBackStack.pop()
        doNavigate(newPath, navigateForwardStack::push)
    }

    override fun navigateForward() {
        val newPath = navigateForwardStack.pop()
        doNavigate(newPath, navigateBackStack::push)
    }

    override fun tryNavigate(pathText: String) {
        val normalized = pathText.replace('\\', '/')
        val path = Paths.get(normalized)
        if (Files.exists(path) && Files.isDirectory(path))
            navigate(SystemFile(path))
        else
            view.failWithMessage("Path should lead to a directory")
    }

    override fun tryOpen(file: DisplayableFile) {
        if (file is SystemFile && desktop != null)
            GlobalScope.launch(Dispatchers.Default) {
                desktop.open(file.path.toFile())
            }
    }

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
                    Left("Can't read contents of the file.")
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

    override fun cancelNavigation() {
        currentJob?.cancel()
    }

    private fun listChildren(file: DisplayableFile, isActive: () -> Boolean): MutableList<DisplayableFile> {
        val resultList = mutableListOf<DisplayableFile>()
        when(file) {
            is SystemFile -> {
                Files.walkFileTree(file.path, emptySet(), 1, object : SimpleFileVisitor<Path>() {
                    override fun visitFile(visitedFile: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (isActive()) {
                            resultList.add(SystemFile(visitedFile,
                                    attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()),
                                    attrs.size()))
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
                                    attrs.size()))
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

    override fun connectToFtp(host: String, port: String, user: String, pass: String) {
        val builder = StringBuilder("ftp://")
        if (user != "") {
            builder.append(user)
            if(pass != "") builder.append(":$pass")
            builder.append('@')
        }
        builder.append(host)
        if (port != "") builder.append(":$port/")

        val url = builder.toString()
        val opts = FileSystemOptions()
        FtpFileSystemConfigBuilder.getInstance().setPassiveMode(opts, true)
        val file = VFS.getManager().resolveFile(url, opts)
        ftpConnected = true
        ftpFileSystem = file.fileSystem
        navigate(FtpFile(file))
        view.ftpConnected()
    }

    override fun disconnectFtp() {
        if (ftpConnected) {
            VFS.getManager().closeFileSystem(ftpFileSystem)
            ftpConnected = false
        }
        view.ftpDisconnected()
    }

    override fun cleanup() {
        if (ftpConnected) {
            VFS.getManager().closeFileSystem(ftpFileSystem)
            ftpConnected = false
        }
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