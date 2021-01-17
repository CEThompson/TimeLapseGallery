package com.vwoom.timelapsegallery.utils

import androidx.room.util.FileUtil
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.InputStream

class FileUtilsTest {

    @Rule
    @JvmField
    val testFolder = TemporaryFolder()

    private lateinit var externalFilesTestDir: File

    @Before
    fun setUp() {
        externalFilesTestDir = testFolder.newFolder("pictures")
    }

    @Test
    fun createTempListPhotoFiles(){
        // Given a project with photos
        val projectEntry = ProjectEntry(1, "name")
        val projectFolder = ProjectUtils.getProjectFolder(externalFilesTestDir, projectEntry)
        projectFolder.mkdirs()
        val photoFile1  = File(projectFolder, "1231.jpeg")
        photoFile1.createNewFile()
        val photoFile2 = File(projectFolder, "12342.jpeg")
        photoFile2.createNewFile()

        // When we use the function
        val textFileList = FileUtils.createTempListPhotoFiles(externalFilesTestDir, projectEntry)

        // A file is returned in the expected format for FFMPEG conversion
        assertTrue(textFileList != null)
        assertTrue(textFileList!!.exists())

        val inputStream: InputStream = textFileList.inputStream()

        val resList: MutableList<String> = ArrayList()

        inputStream.bufferedReader().forEachLine {
            if (it.isNotEmpty()) resList.add(it)
        }

        assert(resList.size == 2)

        assert(resList[0] == "file '${photoFile1.absolutePath}'")
        assert(resList[1] == "file '${photoFile2.absolutePath}'")
    }

    @Test
    fun writeProjectTagsFile(){

        // Given a project and a list of tags
        val projectEntry = ProjectEntry(1, "name")
        var listTags = arrayListOf(TagEntry(1, "a"), TagEntry(1, "b"))

        // Before we write any tags file should not exist
        val metaDir = ProjectUtils.getMetaDirectoryForProject(externalFilesTestDir, projectEntry.id)
        val tagsFile = File(metaDir, FileUtils.TAGS_DEFINITION_TEXT_FILE)
        assertTrue(!tagsFile.exists())

        // When we use the function
        FileUtils.writeProjectTagsFile(externalFilesTestDir, projectEntry.id, listTags)

        // Tags should exist
        assertTrue(tagsFile.exists())

        var inputStream: InputStream = tagsFile.inputStream()
        val tagsRes: MutableList<String> = ArrayList()
        inputStream.bufferedReader().forEachLine {
            if (it.isNotEmpty()) tagsRes.add(it)
        }

        // Tags should represent the input list
        assertTrue(tagsRes.size == 2)
        assertTrue(tagsRes[0] == "a")
        assertTrue(tagsRes[1] == "b")

        // When we use the function again, the format should be updated accordingly
        listTags = arrayListOf(TagEntry(1, "c"), TagEntry(1, "d"))
        FileUtils.writeProjectTagsFile(externalFilesTestDir, projectEntry.id, listTags)

        tagsRes.clear()
        inputStream = tagsFile.inputStream()
        inputStream.bufferedReader().forEachLine {
            if (it.isNotEmpty()) tagsRes.add(it)
        }

        // Should now be c and d
        assertTrue(tagsRes.size == 2)
        assertTrue(tagsRes[0] == "c")
        assertTrue(tagsRes[1] == "d")
    }

    // TODO consider writing test for writing project schedule
    /*
    @Test
    fun writeProjectScheduleFile(){
        FileUtils.writeProjectScheduleFile(externalFilesTestDir, projectId, projectScheduleEntry)
    }*/

    @Test
    fun writeSensorData(){
        // Given photo entries with sensor data yet to exist
        val photoEntry = PhotoEntry(1,1, 1, "100", "1000", "27.1", "5",)
        val photoEntry2 = PhotoEntry(2,1, 2, "1001", "1001", "28.1", "6",)

        val metaDir = ProjectUtils.getMetaDirectoryForProject(externalFilesTestDir, 1)
        val sensorFile = File(metaDir, FileUtils.SENSOR_DEFINITION_TEXT_FILE)

        // Sensor data should not yet exist
        assertTrue(!sensorFile.exists())

        // When we write the data
        FileUtils.writeSensorData(externalFilesTestDir, photoEntry, 1)

        // File should now exist
        assertTrue(sensorFile.exists())

        // When we read the file
        var inputStream: InputStream = sensorFile.inputStream()
        val sensorRes: MutableList<String> = ArrayList()
        inputStream.bufferedReader().forEachLine {
            if (it.isNotEmpty()) sensorRes.add(it)
        }

        // We should have 1 entry with the appropriate parts
        assertTrue(sensorRes.size == 1)
        var parts = sensorRes[0].split(" ")
        assertTrue(parts[0] == "1") // timestamp
        assertTrue(parts[1] == "100") // light
        assertTrue(parts[2] == "27.1") // temp
        assertTrue(parts[3] == "1000") // pressure
        assertTrue(parts[4] == "5") // humidity

        // When we write another entry
        FileUtils.writeSensorData(externalFilesTestDir, photoEntry2, 1)

        // Then it should be appended to the end
        inputStream = sensorFile.inputStream()
        sensorRes.clear()
        inputStream.bufferedReader().forEachLine {
            if (it.isNotEmpty()) sensorRes.add(it)
        }

        // We should have 2 entries now
        assertTrue(sensorRes.size == 2)
        parts = sensorRes[1].split(" ")
        assertTrue(parts[0] == "2") // timestamp
        assertTrue(parts[1] == "1001") // light
        assertTrue(parts[2] == "28.1") // temp
        assertTrue(parts[3] == "1001") // pressure
        assertTrue(parts[4] == "6") // humidity
    }

    @Test
    fun createTemporaryImageFile_givenTempFolder_tempFileExists() {
        // Given the project directory
        val tempFolder = File(externalFilesTestDir, FileUtils.TEMP_FILE_SUBDIRECTORY)
        tempFolder.mkdir()

        // When
        FileUtils.createTemporaryImageFile(tempFolder)

        // Then the temp directory should not be empty
        assertTrue(tempFolder.listFiles()?.size != 0)
    }

    @Test
    fun createFinalFileFromTemp() {
        // Given
        // a pictures directory and temporary_images directory
        val tempFolder = File(externalFilesTestDir, FileUtils.TEMP_FILE_SUBDIRECTORY)
        tempFolder.mkdir()
        // a temporary image file
        val tempFile = FileUtils.createTemporaryImageFile(tempFolder)
        val timestamp = System.currentTimeMillis()
        // and a project
        val projectEntry = ProjectEntry("test project")

        // When we create the folder for the project
        val finalFile = FileUtils.createFinalFileFromTemp(
                externalFilesTestDir,
                tempFile.absolutePath,
                projectEntry,
                timestamp)

        // Then the temp file should have been deleted
        assertTrue(!tempFile.exists())
        // And the final file should exist
        assertTrue(finalFile.exists())
    }

    @Test
    fun deleteTempFiles() {
        // Given a directory of temp files
        val tempFolder = File(externalFilesTestDir, FileUtils.TEMP_FILE_SUBDIRECTORY)
        tempFolder.mkdir()
        // and a picture in the directory
        val child = File(tempFolder, "11.jpg")
        child.createNewFile()

        // When deleteTempFiles() is run
        FileUtils.deleteTempFiles(externalFilesTestDir)

        // Then the directory is gone
        assertTrue(!tempFolder.exists())
        // and the child is gone
        assertTrue(!child.exists())
    }

    @Test
    fun pathContainsReservedCharacter_noReservedCharacters() {
        // Given - a string without any reserved characters
        val testString = "1_Test Project Name"

        // When - the utility is called
        val containsReservedCharacters = FileUtils.pathContainsReservedCharacter(testString)

        // Then - the utility should indicate false: the path does not contain a reserved character
        assertTrue(!containsReservedCharacters)
    }

    @Test
    fun pathContainsReservedCharacter_containsReservedCharacters() {
        // Given - a string with a reserved character
        val testString = "1_Test?Project Name/"

        // When - the utility is called
        val containsReservedCharacters = FileUtils.pathContainsReservedCharacter(testString)

        // Then - the utility should indicate true
        assertTrue(containsReservedCharacters)
    }

    @Test
    fun getPhotoFileNamesFromTimeStamp() {
        // Given a timestamp
        val timestamp: Long = 12345

        // When we call the utility
        val filenames = FileUtils.getPhotoFileExtensions(timestamp)

        // Then an array of three different image extensions should exist
        assertTrue(filenames[0] == "$timestamp.jpg")
        assertTrue(filenames[1] == "$timestamp.png")
        assertTrue(filenames[2] == "$timestamp.jpeg")
    }

}