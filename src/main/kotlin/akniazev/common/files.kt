package akniazev.common

import com.sun.nio.zipfs.ZipPath
import org.apache.commons.vfs2.FileObject
import java.nio.file.Files
import java.nio.file.Path


interface DisplayableFile {
    val name: String
    val extension: String
    val parent: Any?
    val isRoot: Boolean
    val isDirectory: Boolean
}

class SystemFile(val path: Path) : DisplayableFile {
    override val name: String = path.toFile().name
    override val extension: String= path.toFile().extension
    override val parent: Path? = path.parent
    override val isRoot: Boolean = path.parent == null
    override val isDirectory: Boolean = path.toFile().isDirectory
}

class ZipFile(val file: Path) : DisplayableFile {
    override val name: String = file.fileName?.toString()?.substringBeforeLast('/') ?: ""
    override val extension: String = name.substringAfterLast('.', "")
    override val parent: Path? = file.parent
    override val isRoot: Boolean = file.nameCount == 0
    override val isDirectory: Boolean = Files.isDirectory(file)
}

class FtpFile(val file: FileObject) : DisplayableFile {
    override val name: String = file.name.baseName
    override val extension: String = name.substringAfterLast('.', "")
    override val parent: FileObject? = file.parent
    override val isRoot: Boolean = file.parent == null
    override val isDirectory: Boolean = file.isFolder
}