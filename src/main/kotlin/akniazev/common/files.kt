package akniazev.common

import akniazev.controller.FileTypeDetector
import org.apache.commons.vfs2.FileObject
import java.nio.file.Files
import java.nio.file.Path


interface DisplayableFile {
    val name: String
    val parent: DisplayableFile?
    val isDirectory: Boolean
    val isRoot: Boolean get() = parent == null
    val extension: String get() = if (isDirectory) "" else name.substringAfterLast('.', "")
    val type: FileType get() = FileTypeDetector.detect(this)
}

class SystemFile(val path: Path) : DisplayableFile {
    override val name: String = path.toFile().name
    override val isDirectory: Boolean = path.toFile().isDirectory
    override val parent: SystemFile? = if (path.parent != null) SystemFile(path.parent) else null
}

class ZipFile(val file: Path) : DisplayableFile {
    override val name: String = file.fileName?.toString()?.substringBeforeLast('/') ?: ""
    override val isDirectory: Boolean = Files.isDirectory(file)
    override val parent: ZipFile? = if (file.parent != null) ZipFile(file.parent) else null
    override val isRoot: Boolean = file.nameCount == 0
}

class FtpFile(val file: FileObject) : DisplayableFile {
    override val name: String = file.name.baseName
    override val isDirectory: Boolean = file.isFolder
    override val parent: FtpFile? = if (file.parent != null) FtpFile(file.parent) else null
}

enum class FileType {
    DIRECTORY, TEXT, IMAGE, UNKNOWN;
}