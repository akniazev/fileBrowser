# FileBrowser: test assignment for JetBrains

Simple file system manager.  
Can browse through local file system, zip archives and FTP.  
Content preview is available for text and image files.

Project is written in Kotlin with Swing for UI. Background tasks are performed using 
[coroutines](https://github.com/Kotlin/kotlinx.coroutines), 
[Apache Commons VFS](http://commons.apache.org/proper/commons-vfs/) is used  for FTP access and 
[Apache Tika](https://tika.apache.org/) to improve the recognition of text files.

#### Installation and packaging
To package the application use the command `mvn clean compile assembly:single`.  
Compiled jar file will be placed in `/target` directory. Then it can be launched by double click, 
or by using command `java -jar fileBrowser.jar` in the terminal.

#### Usage
To navigate between directories double click on a directory/zip file entry in the table. 
Double click on other entries triggers opening of this file with the default application, 
if the current platform supports [Desktop](https://docs.oracle.com/javase/8/docs/api/java/awt/Desktop.html).
Selecting any entry will display its details in the right panel. For text and images content preview will be shown as well.
Clicking on the table headers triggers sorting by the selected column.

#### Keyboard shortcuts
`Up / Down arrow keys` - navigation in table.  
`Alt + Left arrow` - Back  
`Alt + Right arrow` - Forward  
`Alt + Up arrow / Backspace` - To previous level  
`Enter / Spacebar` - Navigate to directory or open file  

#### Changes since 1.1
- Instead of waiting on the progressbar, all the entries are loaded on the fly. 
User can choose to navigate to the next directory without waiting for the previous one to fully load.
- Improved keyboard navigation.
- Bugfixes and UI tests.

#### Changes since 1.0
- All resources are now processed in separate coroutines, without blocking the main thread.  
- Progress dialog with possibility to cancel the current process.  
- Keyboard navigation support.  
- Entries and be sorted by both filename and extension.  
- Added validation for FTP parameters.  

#### Further improvements  
- OS-specific implementations to get more file attributes.
- Support several FTP connections simultaneously
- Search mechanism.