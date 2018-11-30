package akniazev.common

import akniazev.controller.FileTypeDetector
import org.apache.commons.vfs2.FileObject
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime


/**
 * General representation of file in the application.
 * Almost all the functionality is available through this interface.
 *
 * @see FileType
 * @see FileTypeDetector
 * @author akniazev
 */
interface DisplayableFile {
    val isRoot: Boolean
        get() = parent == null
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "")
    val type: FileType
        get() = FileTypeDetector.detect(this)
    val navigable: Boolean
        get() = isDirectory || extension == "zip"

    val name: String
    val parent: DisplayableFile?
    val isDirectory: Boolean
    val size: Long?
    val lastModified: ZonedDateTime?
    val pathString: String
}


/**
 * Represents a local file.
 *
 * @param path path object, corresponding to this file on local file system
 * @param lastModified datetime of last modification.
 * @param size size of the file
 *
 */
class SystemFile(val path: Path,
                 override val lastModified: ZonedDateTime? = null,
                 override val size: Long? = null) : DisplayableFile {
    override val name: String
        get() = path.toFile().name
    override val isDirectory: Boolean
        get() = path.toFile().isDirectory || parent == null
    override val parent: SystemFile?
        get() = if (path.parent != null) SystemFile(path.parent) else null
    override val pathString: String
        get() = path.toString()
}

/**
 * Represents a file in the zip archive.
 * Keeps a reference to the point in file system, where the archive is stored to facilitate navigation.
 *
 * @param file path to the file within the archive
 * @param systemDir location of the archive
 * @param lastModified datetime of last modification.
 * @param size size of the file
 */
class ZipFile(val file: Path,
              val systemDir: SystemFile,
              override val lastModified: ZonedDateTime? = null,
              override val size: Long? = null) : DisplayableFile {
    override val name: String
        get() = file.fileName?.toString()?.substringBeforeLast('/') ?: ""
    override val isDirectory: Boolean
        get() = Files.isDirectory(file)
    override val parent: DisplayableFile?
        get() = if (file.parent != null) ZipFile(file.parent, systemDir) else systemDir
    override val isRoot: Boolean
        get() = file.nameCount == 0
    override val pathString: String
        get() = file.toString()
}


/**
 * Represents a file on FTP server.
 * @param file underlining file object
 *
 * @see org.apache.commons.vfs2.FileObject
 */
class FtpFile(val file: FileObject) : DisplayableFile {
    override val name: String
        get() = file.name.baseName
    override val isDirectory: Boolean
        get() = file.isFolder
    override val parent: FtpFile?
        get() = if (file.parent != null) FtpFile(file.parent) else null
    override val pathString: String
        get() = file.publicURIString
    override val size: Long?
        get() = if (isDirectory) null else file.content.size
    override val lastModified: ZonedDateTime?
        get() = Instant.ofEpochMilli(file.content.lastModifiedTime).atZone(ZoneId.systemDefault())
}


/**
 * Represents type of a file. Determines whether it should be possible to navigate to file, read it or open it.
 */
enum class FileType {
    DIRECTORY, TEXT, IMAGE, UNKNOWN;
}