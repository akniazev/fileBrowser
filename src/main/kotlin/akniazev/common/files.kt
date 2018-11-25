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
    val navigable: Boolean
        get() = isDirectory || extension == "zip"


    val name: String
    val parent: DisplayableFile?
    val isDirectory: Boolean
    val size: Long
    val lastModified: ZonedDateTime?
    val pathString: String
}

class SystemFile(val path: Path,
                 override val lastModified: ZonedDateTime? = null,
                 override val size: Long = 0) : DisplayableFile {
    override val name: String
        get() = path.toFile().name
    override val isDirectory: Boolean
        get() = path.toFile().isDirectory
    override val parent: SystemFile?
        get() = if (path.parent != null) SystemFile(path.parent) else null
    override val pathString: String
        get() = path.toString()
}

class ZipFile(val file: Path,
              val systemDir: SystemFile,
              override val lastModified: ZonedDateTime? = null,
              override val size: Long = 0) : DisplayableFile {
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

class FtpFile(val file: FileObject) : DisplayableFile {
    override val name: String
        get() = file.name.baseName
    override val isDirectory: Boolean
        get() = file.isFolder
    override val parent: FtpFile?
        get() = if (file.parent != null) FtpFile(file.parent) else null
    override val pathString: String
        get() = file.publicURIString
    override val size: Long
        get() = if (isDirectory) 0 else file.content.size
    override val lastModified: ZonedDateTime?
        get() = Instant.ofEpochMilli(file.content.lastModifiedTime).atZone(ZoneId.systemDefault())
}

enum class FileType {
    DIRECTORY, TEXT, IMAGE, UNKNOWN;
}