# FileBrowser: test assignment for JetBrains

Simple file system manager.  
Can browse through local file system, zip archives and FTP.  
Content preview is available for text and image files.

Project is written in Kotlin with Swing for UI and uses [Apache Commons VFS](http://commons.apache.org/proper/commons-vfs/) for FTP access and 
[Apache Tika](https://tika.apache.org/) to improve the recognition of text files.

Further improvements:
- Use `SwingWorkers` to simulate a progress bar to be displayed to the user when listing files takes some time (especially with FTP)
- Support more filetypes for preview (using Apache Tika, for instance)
- Support several FTP connections simultaneously
- When connecting to FTP, prompt the user for the password
- NOT to store the password in plaintext :)
- Implement search mechanism (i.e. with `Files.walkFileTree`)
