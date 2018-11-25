package akniazev

import akniazev.common.*
import akniazev.controller.ControllerImpl
import org.apache.commons.vfs2.FileSystemOptions
import org.apache.commons.vfs2.VFS
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockftpserver.fake.FakeFtpServer
import org.mockftpserver.fake.UserAccount
import org.mockftpserver.fake.filesystem.DirectoryEntry
import org.mockftpserver.fake.filesystem.FileEntry
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.*


class ControllerTest {

    private var controller: Controller = ControllerImpl()

    @Test fun testTableNavigation() {
        val view = Mockito.mock(View::class.java)
        Mockito.`when`(view.updateFileList(any(DisplayableFile::class.java), anyList())).thenAnswer { invocation ->
            val parent: DisplayableFile? = invocation.getArgument(0)
            assertNotNull(parent)
            assertTrue(parent is SystemFile)
            assertEquals(Paths.get(tempDir), parent.path)

            val result: List<DisplayableFile> = invocation.getArgument(1)
            assertEquals(1, result.size)
            assertEquals("test.txt", result[0].name)
        }
        Mockito.`when`(view.updateAddress(anyString(), anyBoolean())).thenAnswer { invocation ->
            val address: String = invocation.getArgument(0)
            assertEquals(address, dirPath.toString())
        }
        controller.view = view
        controller.navigate(SystemFile(dirPath))
    }

    @Test fun testContent() {
        val view = Mockito.mock(View::class.java)
        Mockito.`when`(view.previewText(anyString())).thenAnswer { invocation ->
            val text: String? = invocation.getArgument(0)
            assertNotNull(text)
            assertEquals("local file content", text)
        }

        controller.view = view
        controller.readText(SystemFile(filePath))
    }

    @Test fun ftpTest() {
        val view = Mockito.mock(View::class.java)
        Mockito.`when`(view.updateFileList(any(), anyList())).thenAnswer { invocation ->
            val parent: DisplayableFile? = invocation.getArgument(0)
            assertNull(parent)

            val result: List<DisplayableFile> = invocation.getArgument(1)
            assertEquals(1, result.size)
            assertEquals("level2", result[0].name)
        }

        controller.view = view
        controller.connectToFtp("localhost", "58562", "user", "password")
        controller.cleanup()
    }

    @Test fun testFtpContent() {
        val view = Mockito.mock(View::class.java)
        Mockito.`when`(view.previewText(anyString())).thenAnswer { invocation ->
            val text: String? = invocation.getArgument(0)
            assertNotNull(text)
            assertEquals("test1 content", text)
        }

        controller.view = view
        val opts = FileSystemOptions()
        FtpFileSystemConfigBuilder.getInstance().setPassiveMode(opts, true)
        val file = VFS.getManager().resolveFile("ftp://user:password@localhost:58562/level2/test.txt", opts)

        controller.readText(FtpFile(file))
        controller.cleanup()
    }

    @Before fun reloadController() {
        controller = ControllerImpl()
    }


    companion object {
        @JvmStatic private val tempDir: String = System.getProperty("java.io.tmpdir")
        @JvmStatic private val dirPath: Path = Paths.get(tempDir, "fileBrowserTest")
        @JvmStatic private val filePath: Path = Paths.get(tempDir, "fileBrowserTest", "test.txt")
        @JvmStatic private val fakeFtpServer = FakeFtpServer()

        @JvmStatic
        @BeforeClass
        fun setup() {
            Files.createDirectory(dirPath)
            Files.createFile(filePath)
            Files.newBufferedWriter(filePath).use { writer ->
                writer.write("local file content")
            }

            fakeFtpServer.addUserAccount(UserAccount("user", "password", "/data"))

            val fileSystem = UnixFakeFileSystem()
            fileSystem.add(DirectoryEntry("/data"))
            fileSystem.add(DirectoryEntry("/data/level2"))
            fileSystem.add(FileEntry("/data/level2/test.txt", "test1 content").apply { lastModified = Date() })
            fakeFtpServer.fileSystem = fileSystem
            fakeFtpServer.serverControlPort = 58562

            fakeFtpServer.start()
            println("FakeFtpServer running on port: ${fakeFtpServer.serverControlPort}")
        }


        @JvmStatic
        @AfterClass
        fun teardown() {
            Files.delete(filePath)
            Files.delete(dirPath)
            fakeFtpServer.stop()
        }
    }



}