package akniazev

import akniazev.controller.ControllerImpl
import akniazev.ui.MainFrame
import org.assertj.swing.core.GenericTypeMatcher
import org.assertj.swing.data.TableCell
import org.assertj.swing.fixture.FrameFixture
import org.assertj.swing.edt.GuiActionRunner
import java.awt.Frame
import java.awt.event.KeyEvent
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager
import org.assertj.swing.timing.Pause.pause
import org.mockftpserver.fake.FakeFtpServer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.*
import org.mockftpserver.fake.UserAccount
import org.mockftpserver.fake.filesystem.DirectoryEntry
import org.mockftpserver.fake.filesystem.FileEntry
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.swing.JDialog
import javax.swing.JLabel


class UITest {

    private lateinit var window: FrameFixture

    @Before fun start() {
        val frame = GuiActionRunner.execute<Frame> { MainFrame(ControllerImpl()) }
        window = FrameFixture(frame)
        window.show()
        navigateToTempDir()
    }

    @After fun stop() {
        window.cleanUp()
    }

    @Test fun testAddressBarNavigation() {
        window.table("table")
                .requireColumnCount(3)
                .requireRowCount(4)
                .requireCellValue(TableCell.row(0).column(1), "directory")
                .requireCellValue(TableCell.row(1).column(1), "image.png")
                .requireCellValue(TableCell.row(2).column(1), "textFile.txt")
                .requireCellValue(TableCell.row(3).column(1), "zipArchive.zip")
    }

    @Test fun testTableNavigation() {
        navigateToInnerDir()
        window.table("table")
                .requireRowCount(3)
                .requireCellValue(TableCell.row(0).column(1), "first.a")
                .requireCellValue(TableCell.row(1).column(1), "second.b")
                .requireCellValue(TableCell.row(2).column(1), "third.c")

        window.textBox("addressBar").requireText(level2Dir.toString())
    }

    @Test fun testSorting() {
        navigateToInnerDir()
        window.table("table").tableHeader().clickColumn(1)
        window.table("table")
                .requireRowCount(3)
                .requireCellValue(TableCell.row(0).column(1), "third.c")
                .requireCellValue(TableCell.row(1).column(1), "second.b")
                .requireCellValue(TableCell.row(2).column(1), "first.a")

        window.table("table").tableHeader().clickColumn(2)
        window.table("table")
                .requireRowCount(3)
                .requireCellValue(TableCell.row(0).column(2), "a")
                .requireCellValue(TableCell.row(1).column(2), "b")
                .requireCellValue(TableCell.row(2).column(2), "c")
    }

    @Test fun testNavigationButtons() {
        navigateToInnerDir()
        val addressBar = window.textBox("addressBar")
        addressBar.requireText(level2Dir.toString())
        window.button("upBtn").click()
        addressBar.requireText(testDir.toString())
        window.button("backBtn").click()
        addressBar.requireText(level2Dir.toString())
        window.button("backBtn").click()
        addressBar.requireText(testDir.toString())
        window.button("forwardBtn").click()
        addressBar.requireText(level2Dir.toString())

    }

    @Test fun testKeyboardNavigation() {
        window.table("table")
                .selectRows(0)
                .pressAndReleaseKeys(KeyEvent.VK_DOWN)
                .requireSelectedRows(1)
                .pressAndReleaseKeys(KeyEvent.VK_UP)
                .requireSelectedRows(0)
                .pressAndReleaseKeys(KeyEvent.VK_ENTER)
        window.textBox("addressBar").requireText(level2Dir.toString())

        window.table("table")
                .pressKey(KeyEvent.VK_ALT)
                .pressKey(KeyEvent.VK_LEFT)
                .releaseKey(KeyEvent.VK_LEFT)
                .releaseKey(KeyEvent.VK_ALT)
        window.textBox("addressBar").requireText(testDir.toString())

        window.table("table")
                .pressKey(KeyEvent.VK_ALT)
                .pressKey(KeyEvent.VK_RIGHT)
                .releaseKey(KeyEvent.VK_RIGHT)
                .releaseKey(KeyEvent.VK_ALT)
        window.textBox("addressBar").requireText(level2Dir.toString())

        window.table("table")
                .pressKey(KeyEvent.VK_ALT)
                .pressKey(KeyEvent.VK_UP)
                .releaseKey(KeyEvent.VK_UP)
                .releaseKey(KeyEvent.VK_ALT)
        window.textBox("addressBar").requireText(testDir.toString())
    }

    @Test fun testFiltering() {
        window.table("table").requireRowCount(4)
        window.comboBox("extensionFilter")
                .replaceText("txt")
                .pressAndReleaseKeys(KeyEvent.VK_ENTER)

        window.table("table")
                .requireRowCount(2)
                .requireCellValue(TableCell.row(0).column(1), "directory")
                .requireCellValue(TableCell.row(1).column(1), "textFile.txt")

        window.comboBox("extensionFilter")
                .replaceText("Show all files")
                .pressAndReleaseKeys(KeyEvent.VK_ENTER)

        window.table("table").requireRowCount(4)
    }

    @Test fun testNavigationFail() {
        window.textBox("addressBar")
                .enterText(File.separator)
                .enterText("wrong")
                .pressAndReleaseKeys(KeyEvent.VK_ENTER)

        window.dialog()
                .requireVisible()
                .requireModal()
                .label(object : GenericTypeMatcher<JLabel>(JLabel::class.java) {
                    override fun isMatching(label: JLabel?): Boolean {
                        return label?.text != null
                    }
                })
                .requireText("Path should lead to a directory.")
    }

    @Test fun testTextPreview() {
        window.table("table")
                .requireCellValue(TableCell.row(2).column(1), "textFile.txt")
                .cell(TableCell.row(2).column(1)).click()
        window.label("previewNameLabel")
                .requireVisible()
                .requireText("textFile.txt")
        window.textBox("textPreview")
                .requireVisible()
                .requireText("Test file content")
    }

    @Test fun testImagePreview() {
        window.table("table")
                .requireCellValue(TableCell.row(1).column(1), "image.png")
                .cell(TableCell.row(1).column(1)).click()
        pause(500)

        window.label("previewNameLabel")
                .requireVisible()
                .requireText("image.png")
        window.label("previewImage").requireVisible()

    }

    @Test fun testZipFileNavigation() {
        navigateToZipFile()

        window.table("table")
                .requireRowCount(4)
                .requireCellValue(TableCell.row(0).column(1), "emptyDirectory")
                .requireCellValue(TableCell.row(1).column(1), "first.txt")
                .requireCellValue(TableCell.row(2).column(1), "second.txt")
                .requireCellValue(TableCell.row(3).column(1), "third.png")
        window.table("table")
                .cell(TableCell.row(0).column(1))
                .doubleClick()
        pause(200)
        window.table("table").requireRowCount(0)
        window.textBox("addressBar")
                .requireDisabled()
                .requireText("/emptyDirectory/")

        window.button("upBtn").click()
        pause(200)
        window.table("table").requireRowCount(4)
        window.textBox("addressBar")
                .requireDisabled()
                .requireText("/")

        window.button("upBtn").click()
        pause(200)
        window.table("table").requireRowCount(4)
        window.textBox("addressBar")
                .requireEnabled()
                .requireText(testDir.toString())

    }

    @Test fun testZipFileTextPreview() {
        navigateToZipFile()

        window.table("table")
                .requireCellValue(TableCell.row(1).column(1), "first.txt")
                .cell(TableCell.row(1).column(1)).click()

        window.label("previewNameLabel")
                .requireVisible()
                .requireText("first.txt")
        window.textBox("textPreview")
                .requireVisible()
                .requireText("first content")

        window.table("table")
                .requireCellValue(TableCell.row(2).column(1), "second.txt")
                .cell(TableCell.row(2).column(1)).click()

        window.label("previewNameLabel")
                .requireVisible()
                .requireText("second.txt")
        window.textBox("textPreview")
                .requireVisible()
                .requireText("second content")
    }


    @Test fun testZipFileImagePreview() {
        navigateToZipFile()

        window.table("table")
                .requireCellValue(TableCell.row(3).column(1), "third.png")
                .cell(TableCell.row(3).column(1)).click()
        pause(500)

        window.label("previewNameLabel")
                .requireVisible()
                .requireText("third.png")
        window.label("previewImage").requireVisible()
    }

    @Test fun testFtpValidation() {
        window.button("connectFtpBtn").click()
        val connectBtn = window.dialog("connectFtpDialog").button("connectBtn")
        window.dialog("connectFtpDialog").requireVisible()
        connectBtn.requireDisabled()

        val host = window.dialog("connectFtpDialog").textBox("host")
        val port = window.dialog("connectFtpDialog").textBox("port")
        val user = window.dialog("connectFtpDialog").textBox("user")
        val password = window.dialog("connectFtpDialog").textBox("password")

        // enabled -> minimal valid params
        host.enterText("localhost")
        port.enterText("21")
        connectBtn.requireEnabled()

        // disabled -> invalid port
        port.deleteText()
        port.enterText("invalid")
        connectBtn.requireDisabled()

        //disabled -> invalid hostname
        port.deleteText()
        port.enterText("21")
        host.deleteText()
        host.enterText("invalid")
        connectBtn.requireDisabled()

        // enabled -> valid hostname
        host.deleteText()
        host.enterText("hostname.domain")
        connectBtn.requireEnabled()

        // enabled -> valid ip address
        host.deleteText()
        host.enterText("127.0.0.0")
        connectBtn.requireEnabled()

        // disabled -> port num too high
        port.deleteText()
        port.enterText("655350")
        connectBtn.requireDisabled()

        // disabled -> password without username
        port.deleteText()
        port.enterText("21")
        password.enterText("smth")
        connectBtn.requireDisabled()

        // enabled -> all fields valid
        user.enterText("username")
        connectBtn.requireEnabled()

        // enabled -> password is optional
        user.deleteText()
        password.deleteText()
        user.enterText("username")
        connectBtn.requireEnabled()
    }

    @Test fun testFtpNavigation() {
        val ftpUrl = "ftp://user:***@localhost:58562/"
        connectToFtp()
        window.table("table")
                .requireRowCount(3)
                .requireCellValue(TableCell.row(0).column(1), "dir1")
                .requireCellValue(TableCell.row(1).column(1), "dir2")
                .requireCellValue(TableCell.row(2).column(1), "dir3")
        window.textBox("addressBar")
                .requireDisabled()
                .requireText(ftpUrl)
        window.table("table")
                .cell(TableCell.row(0).column(1))
                .doubleClick()
        pause(500)

        window.table("table")
                .requireRowCount(1)
                .requireCellValue(TableCell.row(0).column(1), "test.txt")
        window.textBox("addressBar").requireText(ftpUrl + "dir1")
        window.button("upBtn").click()
        pause(500)

        window.table("table").requireRowCount(3)
        window.textBox("addressBar").requireText(ftpUrl)
    }

    @Test fun testFtpConnectionFail() {
        window.button("connectFtpBtn").click()
        window.dialog("connectFtpDialog").textBox("host").enterText("localhost")
        window.dialog("connectFtpDialog").textBox("port").enterText("58562")
        window.dialog("connectFtpDialog").textBox("user").enterText("wrong")
        window.dialog("connectFtpDialog").textBox("password").enterText("pass")

        window.dialog("connectFtpDialog").button("connectBtn")
                .requireEnabled()
                .click()
        pause(1000)

        window.dialog(object : GenericTypeMatcher<JDialog>(JDialog::class.java) {
                override fun isMatching(dialog: JDialog?): Boolean {
                    return dialog?.title == "Error"
                }
                })
                .requireVisible()
                .requireModal()
                .label(object : GenericTypeMatcher<JLabel>(JLabel::class.java) {
                    override fun isMatching(label: JLabel?): Boolean {
                        return label?.text != null
                    }
                })
                .requireText("Can't connect to FTP with provided data.")
    }

    @Test fun testFtTextPreview() {
        connectToFtp()
        window.table("table")
                .cell(TableCell.row(0).column(1))
                .doubleClick()
        pause(500)
        window.table("table").selectRows(0)

        window.label("previewNameLabel")
                .requireVisible()
                .requireText("test.txt")
        window.textBox("textPreview")
                .requireVisible()
                .requireText("test content")
    }

    @Test fun testFtpImagePreview() {
        connectToFtp()

        window.table("table")
                .requireCellValue(TableCell.row(1).column(1), "dir2")
                .cell(TableCell.row(1).column(1))
                .doubleClick()
        pause(500)

        window.table("table")
                .requireCellValue(TableCell.row(0).column(1), "image.png")
                .cell(TableCell.row(0).column(1)).click()
        pause(500)

        window.label("previewNameLabel")
                .requireVisible()
                .requireText("image.png")
        window.label("previewImage").requireVisible()
    }

    private fun navigateToTempDir() {
        window.textBox("addressBar").deleteText()
        window.textBox("addressBar").setText(testDir.toString())
        window.textBox("addressBar").pressAndReleaseKeys(KeyEvent.VK_ENTER)
    }

    private fun navigateToInnerDir() {
        window.table("table").cell(TableCell.row(0).column(1)).doubleClick()
    }

    private fun navigateToZipFile() {
        window.table("table")
                .requireCellValue(TableCell.row(3).column(1), "zipArchive.zip")
                .cell(TableCell.row(3).column(1)).doubleClick()
        pause(500)
    }

    private fun connectToFtp() {
        window.button("connectFtpBtn").click()
        window.dialog("connectFtpDialog").textBox("host").enterText("localhost")
        window.dialog("connectFtpDialog").textBox("port").enterText("58562")
        window.dialog("connectFtpDialog").textBox("user").enterText("user")
        window.dialog("connectFtpDialog").textBox("password").enterText("password")

        window.dialog("connectFtpDialog").button("connectBtn")
                .requireEnabled()
                .click()
        pause(1000)
    }

    companion object {

        @JvmStatic private val tempDir: String = System.getProperty("java.io.tmpdir")
        @JvmStatic private val testDir: Path = Paths.get(tempDir, "fileBrowserTest")
        @JvmStatic private val level2Dir: Path = Paths.get(tempDir, "fileBrowserTest", "directory")
        @JvmStatic private val fakeFtpServer = FakeFtpServer()

        @JvmStatic
        @BeforeClass
        fun setup() {
            // Fails the test if UI is being accessed from something other that EDT.
            FailOnThreadViolationRepaintManager.install()

            val textFile: Path = Paths.get(tempDir, "fileBrowserTest", "textFile.txt")
            val image: Path = Paths.get(tempDir, "fileBrowserTest", "image.png")
            val archive: Path = Paths.get(tempDir, "fileBrowserTest", "zipArchive.zip")
            val first: Path = Paths.get(tempDir, "fileBrowserTest", "directory", "first.a")
            val second: Path = Paths.get(tempDir, "fileBrowserTest", "directory", "second.b")
            val third: Path = Paths.get(tempDir, "fileBrowserTest", "directory", "third.c")

            if (Files.exists(testDir))
                deleteTestDir()

            Files.createDirectory(testDir)
            Files.createDirectory(level2Dir)
            Files.createFile(textFile)
            Files.newBufferedWriter(textFile).use { writer ->
                writer.write("Test file content")
            }
            Files.createFile(first)
            Files.createFile(second)
            Files.createFile(third)

            val resourceImage = Paths.get(javaClass.getResource("/image.png").toURI())
            Files.copy(resourceImage, image)

            ZipOutputStream(FileOutputStream(archive.toFile())).use {
                it.putNextEntry(ZipEntry("emptyDirectory/"))
                it.closeEntry()
                it.putNextEntry(ZipEntry("first.txt"))
                it.write("first content".toByteArray())
                it.closeEntry()
                it.putNextEntry(ZipEntry("second.txt"))
                it.write("second content".toByteArray())
                it.closeEntry()
                it.putNextEntry(ZipEntry("third.png"))
                Files.copy(image, it)
                it.closeEntry()
            }

            fakeFtpServer.addUserAccount(UserAccount("user", "password", "/"))

            val fileSystem = UnixFakeFileSystem()
            fileSystem.add(DirectoryEntry("/"))
            fileSystem.add(DirectoryEntry("/dir1"))
            fileSystem.add(DirectoryEntry("/dir2"))
            fileSystem.add(DirectoryEntry("/dir3"))
            fileSystem.add(FileEntry("/dir1/test.txt", "test content").apply { lastModified = Date() })
            val imageEntry = FileEntry("/dir2/image.png")
            Files.copy(image, imageEntry.createOutputStream(true))
            fileSystem.add(imageEntry)
            fakeFtpServer.fileSystem = fileSystem
            fakeFtpServer.serverControlPort = 58562

            fakeFtpServer.start()
            println("FakeFtpServer running on port: ${fakeFtpServer.serverControlPort}")
        }


        @JvmStatic
        @AfterClass
        fun teardown() {
            deleteTestDir()
            fakeFtpServer.stop()
        }

        @JvmStatic
        private fun deleteTestDir() {
            Files.walk(testDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach { it.delete() }
        }
    }

}