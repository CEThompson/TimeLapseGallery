package com.vwoom.timelapsegallery.utils

import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
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

    /* Tests retrieving a list of photo entries from a project folder*/
    @Test
    fun getPhotoEntriesInProjectDirectory_directoryWithData_returnsTwoOrderedPhotoEntries() {
        // Given:
        // a project to test
        val projectName = "test project"
        val projectEntry = ProjectEntry(1, projectName)
        // a project directory
        val projectFolder = File(externalFilesTestDir, "1_$projectName")
        projectFolder.mkdir()
        // and files in the directory
        File(projectFolder, "99999999.jpg").createNewFile()
        File(projectFolder, "11111111.jpg").createNewFile()
        val metaFolder = File(projectFolder, "meta")
        metaFolder.mkdir()
        File(metaFolder, "tags.txt").createNewFile()

        // When we get the photo entries from the method
        val listOfPhotoEntries = FileUtils.getPhotoEntriesInProjectDirectory(externalFilesTestDir, projectEntry)

        // Then assert that the list is as expected
        val expectedList = listOf(
                PhotoEntry(1,11111111),
                PhotoEntry(1,99999999))
        assertThat(listOfPhotoEntries, `is`(expectedList))
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
        val finalFile = FileUtils.createFinalFileFromTemp(externalFilesTestDir, tempFile.absolutePath, projectEntry, timestamp)

        // Then the temp file should have been deleted
        assert(!tempFile.exists())
        // And the final file should exist
        assert(finalFile.exists())
    }

    @Test
    fun renameProject() {
        // Given
        // a project to test
        val projectName = "second test project"
        val id: Long = 2
        val projectEntry = ProjectEntry(id, projectName)
        // a project directory
        val projectFolder = File(externalFilesTestDir, "${id}_$projectName")
        projectFolder.mkdir()
        // files in the directory
        val children = listOf("99999999.jpg", "11111111.jpg")
        for (child in children) File(projectFolder, child).createNewFile()

        // and project entry to rename it to
        val projectRenameString = "test rename"
        val projectEntryToRename = ProjectEntry(id, projectRenameString)

        // When we run the function renameProject
        FileUtils.renameProject(externalFilesTestDir, projectEntry, projectEntryToRename)

        // Then we expect the previous folder to be gone
        assert(!projectFolder.exists())
        // and the renamed folder to exist
        val renamedProject = File(externalFilesTestDir, "${id}_$projectRenameString")
        assert(renamedProject.exists())
        // with the same children
        for (child in children) assert(File(renamedProject, child).exists())
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
    fun deleteProject() {
        // Given
        // a project to test
        val projectName = "delete test project"
        val id: Long = 3
        val projectEntry = ProjectEntry(id, projectName)
        // a project directory
        val projectFolder = File(externalFilesTestDir, "${id}_$projectName")
        projectFolder.mkdir()
        // files in the directory
        val first = File(projectFolder, "99999999.jpg")
        first.createNewFile()
        val second = File(projectFolder, "11111111.jpg")
        second.createNewFile()

        // When deleteProject() is run
        FileUtils.deleteProject(externalFilesTestDir, projectEntry)

        // Then the project folder and its children no longer exist
        assert(!first.exists())
        assert(!second.exists())
        assert(!projectFolder.exists())
    }

    @Test
    fun deletePhoto() {
        // Given a project to test
        val projectName = "delete photo test project"
        val id: Long = 4
        val projectEntry = ProjectEntry(id, projectName)
        // a project directory
        val projectFolder = File(externalFilesTestDir, "${id}_$projectName")
        projectFolder.mkdir()
        // two photos in the directory
        val firstPhotoTimestamp: Long = 999999999
        val first = File(projectFolder, "${firstPhotoTimestamp}.jpg")
        first.createNewFile()
        val second = File(projectFolder, "11111111.jpg")
        second.createNewFile()
        // and a photo entry derived for the first photo
        val firstPhotoEntry = PhotoEntry(projectEntry.id, firstPhotoTimestamp)

        // When deleting the photo
        FileUtils.deletePhoto(externalFilesTestDir, projectEntry, firstPhotoEntry)

        // Then first photo no longer exists
        assert(!first.exists())
        // but the second does exist
        assert(second.exists())
    }

    @Test
    fun pathContainsReservedCharacter_noReservedCharacters() {
        // Given - a string without any reserved characters
        val testString = "1_Test Project Name"

        // When - the utility is called
        val containsReservedCharacters = FileUtils.pathContainsReservedCharacter(testString)

        // Then - the utility should indicate false: the path does not contain a reserved character
        assertFalse(containsReservedCharacters)
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
    fun getPhotoUrl(){
        // Given a project to test
        val projectName = "get photo url test project"
        val id: Long = 5
        // a project directory
        val projectFolder = File(externalFilesTestDir, "${id}_$projectName")
        projectFolder.mkdir()
        // a photo in the directory
        val timestamp: Long = 999999999
        val first = File(projectFolder, "${timestamp}.jpg")
        first.createNewFile()
        // and entries derived for photo and project
        val photoEntry = PhotoEntry(id, id, timestamp)
        val projectEntry = ProjectEntry(id, projectName)

        // When the utility gets the entry
        val photoUrl = FileUtils.getPhotoUrl(externalFilesTestDir, projectEntry, photoEntry.timestamp)

        // Then the returned path should be the same as the created path
        assert(photoUrl == first.absolutePath)
    }

    @Test
    fun getPhotoFileNamesFromTimeStamp() {
        // Given a timestamp
        val timestamp: Long = 12345

        // When we call the utility
        val filenames = FileUtils.getPhotoFileNames(timestamp)

        // Then an array of three different image extensions should exist
        assert(filenames[0] == "$timestamp.jpg")
        assert(filenames[1] == "$timestamp.png")
        assert(filenames[2] == "$timestamp.jpeg")
    }

    @Test
    fun getMetaDirectoryForProject() {
        // given a project
        val project = ProjectEntry(5, "test name")

        // when we get the directory for the project
        val metaDir = FileUtils.getMetaDirectoryForProject(externalFilesTestDir, project.id)
        // and get the relative path
        val absPath = metaDir.absolutePath
        val relPath = absPath.substring(metaDir.absolutePath.lastIndexOf(File.separatorChar)+1)

        // Then the meta directory exists
        assert(metaDir.exists())
        // the relative path is equal to the project ID (ex. "meta/5")
        assert(relPath == project.id.toString())
    }

    companion object {
        private val TAG = FileUtilsTest::class.java.simpleName
    }
}