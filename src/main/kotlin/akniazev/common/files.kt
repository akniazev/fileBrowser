package akniazev.common

import com.sun.nio.zipfs.ZipPath
import org.apache.commons.vfs2.FileObject
import java.nio.file.Files
import java.nio.file.Path


interface DisplayableFile {
    val name: String
    val parent: DisplayableFile?
    val isDirectory: Boolean
    val isRoot: Boolean get() = parent == null
    val extension: String get() = if (isDirectory) "" else name.substringAfterLast('.', "")
    val type: FileType get() = if (isDirectory) FileType.DIRECTORY else FileType.fromExtension(extension)
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

    companion object {
        private val textFiles = setOf("txt", "js", "log", "json")
        private val imageFiles = setOf("png", "jpeg", "jpg")
        fun fromExtension(ext: String): FileType = when(ext.toLowerCase()) {
            in textFiles -> TEXT
            in imageFiles -> IMAGE
            else -> UNKNOWN
        }
    }
}