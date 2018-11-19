package akniazev.common

import akniazev.controller.FileTypeDetector
import org.apache.commons.vfs2.FileObject
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

interface DisplayableFile {
    val isRoot: Boolean
        get() = parent == null
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "")
    val type: FileType
        get() = FileTypeDetector.detect(this)


    val name: String
    val parent: DisplayableFile?
    val isDirectory: Boolean
    val size: Long
    val lastModified: ZonedDateTime?
}

class SystemFile(val path: Path, override val lastModified: ZonedDateTime? = null, override val size: Long = 0) : DisplayableFile {
    override val name: String = path.toFile().name
    override val isDirectory: Boolean = path.toFile().isDirectory
    override val parent: SystemFile? = if (path.parent != null) SystemFile(path.parent) else null
}

class ZipFile(val file: Path, override val lastModified: ZonedDateTime? = null, override val size: Long = 0) : DisplayableFile {
    override val name: String = file.fileName?.toString()?.substringBeforeLast('/') ?: ""
    override val isDirectory: Boolean = Files.isDirectory(file)
    override val parent: ZipFile? = if (file.parent != null) ZipFile(file.parent) else null
    override val isRoot: Boolean = file.nameCount == 0
}

class FtpFile(val file: FileObject) : DisplayableFile {
    override val name: String = file.name.baseName
    override val isDirectory: Boolean = file.isFolder
    override val parent: FtpFile? = if (file.parent != null) FtpFile(file.parent) else null

    override val size: Long = if (isDirectory) 0 else file.content.size
    override val lastModified: ZonedDateTime?
        get() = Instant.ofEpochMilli(file.content.lastModifiedTime).atZone(ZoneId.systemDefault())
}

enum class FileType {
    DIRECTORY, TEXT, IMAGE, UNKNOWN;
}