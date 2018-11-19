package akniazev.controller

import akniazev.common.DisplayableFile
import akniazev.common.SystemFile
import akniazev.common.ZipFile
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.time.ZoneId

class SystemFileVisitor(private val list: MutableList<DisplayableFile>) : SimpleFileVisitor<Path>() {
    override fun visitFile(visitedFile: Path, attrs: BasicFileAttributes): FileVisitResult {
        list.add(SystemFile(visitedFile,
                            attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()),
                            attrs.size()))
        return FileVisitResult.CONTINUE
    }
}

class ZipFileVisitor(private val list: MutableList<DisplayableFile>) : SimpleFileVisitor<Path>() {
    override fun visitFile(visitedFile: Path, attrs: BasicFileAttributes): FileVisitResult {
        list.add(ZipFile(visitedFile,
                            attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()),
                            attrs.size()))
        return FileVisitResult.CONTINUE
    }
}