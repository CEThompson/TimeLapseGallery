package com.vwoom.timelapsegallery.utils

import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileUtilsTest {

    @Rule @JvmField
    val testFolder = TemporaryFolder()

    private lateinit var externalFilesTestDir: File

    @Before
    fun setUp(){
        externalFilesTestDir = testFolder.newFolder("pictures")
    }

    @Test
    fun createTemporaryImageFile_givenTempFolder_tempFileExists() {
        // Given the project directory
        val tempFolder = File(externalFilesTestDir, FileUtils.TEMP_FILE_SUBDIRECTORY)
        tempFolder.mkdir()

        // When
        FileUtils.createTemporaryImageFile(tempFolder)

        // Then the temp directory should not be empty
        assert(tempFolder.listFiles()?.size!=0)
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
        assert(!tempFile.exists())
        // And the final file should exist
        assert(finalFile.exists())
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
        assert(!tempFolder.exists())
        // and the child is gone
        assert(!child.exists())
    }

    @Test
    fun pathContainsReservedCharacter_noReservedCharacters() {
        // Given - a string without any reserved characters
        val testString = "1_Test Project Name"

        // When - the utility is called
        val containsReservedCharacters = FileUtils.pathContainsReservedCharacter(testString)

        // Then - the utility should indicate false: the path does not contain a reserved character
        assert(!containsReservedCharacters)
    }

    @Test
    fun pathContainsReservedCharacter_containsReservedCharacters() {
        // Given - a string with a reserved character
        val testString = "1_Test?Project Name/"

        // When - the utility is called
        val containsReservedCharacters = FileUtils.pathContainsReservedCharacter(testString)

        // Then - the utility should indicate true
        assert(containsReservedCharacters)
    }

    @Test
    fun getPhotoFileNamesFromTimeStamp() {
        // Given a timestamp
        val timestamp: Long = 12345

        // When we call the utility
        val filenames = FileUtils.getPhotoFileExtensions(timestamp)

        // Then an array of three different image extensions should exist
        assert(filenames[0] == "$timestamp.jpg")
        assert(filenames[1] == "$timestamp.png")
        assert(filenames[2] == "$timestamp.jpeg")
    }

    companion object {
        private val TAG = FileUtilsTest::class.java.simpleName
    }
}