package akniazev

import akniazev.controller.ControllerImpl
import akniazev.ui.MainFrame
import org.apache.commons.vfs2.VFS
import org.mockftpserver.fake.FakeFtpServer
import org.mockftpserver.fake.UserAccount
import org.mockftpserver.fake.filesystem.DirectoryEntry
import org.mockftpserver.fake.filesystem.FileEntry
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem
import javax.swing.SwingUtilities

val fakeFtpServer = FakeFtpServer()

fun main(args: Array<String>) {
    println("has ftp: ${VFS.getManager().hasProvider("ftp")}")
    setup()
    SwingUtilities.invokeLater {
        MainFrame(ControllerImpl())
    }

}

fun setup() {
    fakeFtpServer.addUserAccount(UserAccount("user", "password", "/data"))

    val fileSystem = UnixFakeFileSystem()
    fileSystem.add(DirectoryEntry("/data"))
    fileSystem.add(DirectoryEntry("/data/level2"))
    fileSystem.add(DirectoryEntry("/data/level2/level2.1"))
    fileSystem.add(DirectoryEntry("/data/level2/level2.2"))
    fileSystem.add(DirectoryEntry("/data/level2/level2.3"))
    fileSystem.add(DirectoryEntry("/data/level2/level3"))
    fileSystem.add(DirectoryEntry("/data/level2/level3/level3.1"))
    fileSystem.add(DirectoryEntry("/data/level2/level3/level3.2"))
    fileSystem.add(FileEntry("/data/level2/level3/level3.2/wow.txt", "abcdef 1234567890"))
    fileSystem.add(FileEntry("/data/level2/level3/level3.2/wow2.txt", "abcdef 1234567890"))
    fileSystem.add(FileEntry("/data/level2/level3/level3.2/wow3.txt", "abcdef 1234567890"))
    fileSystem.add(FileEntry("/data/foobar.txt", "abcdef 1234567890"))
    fileSystem.add(FileEntry("/data/level2/level3/text.txt", "abcdef 1234567890"))
    fakeFtpServer.setFileSystem(fileSystem)
    fakeFtpServer.serverControlPort = 58562

    fakeFtpServer.start()
    println(fakeFtpServer.serverControlPort)
}