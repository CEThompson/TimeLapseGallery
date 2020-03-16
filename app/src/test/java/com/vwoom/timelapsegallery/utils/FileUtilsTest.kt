package com.vwoom.timelapsegallery.utils

import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
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

    // TODO lock down naming for tests
    // TODO use given/when/then structure for tests
    // TODO consider assertion framework

    @Rule @JvmField
    val testFolder = TemporaryFolder()

    private lateinit var externalFilesTestDir: File

    @Before
    fun setUp(){
        externalFilesTestDir = testFolder.newFolder("pictures")
    }

    /* Tests retrieving a list of photo entries from a project folder*/
    @Test
    fun getPhotoEntriesInProjectDirectory_directoryWithImagesTextAndFolder_twoOrderedPhotoEntries() {
        /* Given */
        // Create the project to test
        val projectName = "test project"
        val projectEntry = ProjectEntry(1, projectName)

        // Create the project directory
        val projectFolder = File(externalFilesTestDir, "1_$projectName")
        projectFolder.mkdir()

        // Create files in the directory
        File(projectFolder, "99999999.jpg").createNewFile()
        File(projectFolder, "11111111.jpg").createNewFile()
        val metaFolder = File(projectFolder, "meta")
        metaFolder.mkdir()
        File(metaFolder, "tags.txt").createNewFile()

        /* Actually use the method we are testing with mocked data */
        /* When */
        // TODO this utility fails when the test environment is a windows system, figure out how to fix this
        val listOfPhotoEntries = FileUtils.getPhotoEntriesInProjectDirectory(externalFilesTestDir, projectEntry)

        val expectedList = listOf(
                PhotoEntry(1,11111111),
                PhotoEntry(1,99999999))

        /* Then */
        /* Assert out response is equal to the expectation */
        assertThat(listOfPhotoEntries, `is`(expectedList))

        // TODO figure out logging for tests
        //System.out.println("$TAG $projectFolder")
        //System.out.println("$TAG ${listOfPhotoEntries == null}")
        //System.out.println("$TAG returned list is $listOfPhotoEntries")
        //System.out.println("$TAG test list is $assertionList")
    }

    @Test
    fun createTemporaryImageFile_tempImageFolder_tempFileShouldExist() {
        /* Given */
        // Create the project directory
        val tempFolder = File(externalFilesTestDir, FileUtils.TEMP_FILE_SUBDIRECTORY)
        tempFolder.mkdir()

        /* When */
        FileUtils.createTemporaryImageFile(tempFolder)

        /* Then */
        // The temp directory should not be empty
        assert(tempFolder.listFiles()?.size!=0)
    }

    @Test
    fun createFinalFileFromTempTest_shouldPass() {
        // Create the pictures directory and temporary_images directory
        val tempFolder = File(externalFilesTestDir, FileUtils.TEMP_FILE_SUBDIRECTORY)
        tempFolder.mkdir()

        // Create the temporary image file
        val tempFile = FileUtils.createTemporaryImageFile(tempFolder)
        val timestamp = System.currentTimeMillis()

        // Create the project
        val projectEntry = ProjectEntry("test project")

        // Create the folder for the project
        val finalFile = FileUtils.createFinalFileFromTemp(externalFilesTestDir, tempFile.absolutePath, projectEntry, timestamp)

        assert(!tempFile.exists()) // make sure temp file was deleted
        assert(finalFile != null) // make sure final file was created
        assert(finalFile.exists())
        // TODO make other assertions about the final copied file

        // TODO convert to logs
        System.out.println("$TAG tempFile path $finalFile")
        System.out.println("$TAG finalFile path $tempFile")
        System.out.println("$TAG tempFile exists ${tempFile.exists()}")
        System.out.println("$TAG finalFile exists ${finalFile.exists()}")

    }

    @Test
    fun renameProject() {
        /* Given - A named project that has files */
        // Create the project to test
        val projectName = "second test project"
        val id: Long = 2
        val projectEntry = ProjectEntry(id, projectName)

        // Create the project directory
        val projectFolder = File(externalFilesTestDir, "${id}_$projectName")
        projectFolder.mkdir()

        // Create files in the directory
        File(projectFolder, "99999999.jpg").createNewFile()
        File(projectFolder, "11111111.jpg").createNewFile()
        File(projectFolder, "videos").mkdir()
        File(projectFolder, "tags.txt").createNewFile()

        // Create the project entry to rename it to
        val projectRenameString = "test rename"
        val projectEntryToRename = ProjectEntry(id, projectRenameString)

        /* When - We run the function renameProject */
        FileUtils.renameProject(externalFilesTestDir, projectEntry, projectEntryToRename)

        /* Then - Expect the previous folder to be gone, a new folder with the same files to exist */
        assert(File(externalFilesTestDir, "${id}_$projectRenameString").exists())
    }

    @Test
    fun deleteTempFiles() {
        /* Given - A directory of temp files */
        val tempFolder = File(externalFilesTestDir, FileUtils.TEMP_FILE_SUBDIRECTORY)
        tempFolder.mkdir()

        File(tempFolder, "11.jpg")
        /* When - deleteTempFiles() is run */
        FileUtils.deleteTempFiles(externalFilesTestDir)

        /* Then - the directory is empty / gone */
        assert(!tempFolder.exists())
    }

    @Test
    fun deleteProject() {
        /* Given an existing project */
        // Create the project to test
        val projectName = "delete test project"
        val id: Long = 3
        val projectEntry = ProjectEntry(id, projectName)

        // Create the project directory
        val projectFolder = File(externalFilesTestDir, "${id}_$projectName")
        projectFolder.mkdir()

        // Create files in the directory
        val first = File(projectFolder, "99999999.jpg")
        first.createNewFile()
        val second = File(projectFolder, "11111111.jpg")
        second.createNewFile()
        val third = File(projectFolder, "videos")
        third.mkdir()
        val fourth = File(projectFolder, "tags.txt")
        fourth.createNewFile()

        /* When deleteProject() is run */
        FileUtils.deleteProject(externalFilesTestDir, projectEntry)

        /* Project folder no longer exists */
        assert(!first.exists())
        assert(!second.exists())
        assert(!third.exists())
        assert(!fourth.exists())
        assert(!projectFolder.exists())
    }

    @Test
    fun deletePhoto() {
        // Given - a photo belonging to a project
        // Create the project to test
        val projectName = "delete photo test project"
        val id: Long = 4
        val projectEntry = ProjectEntry(id, projectName)

        // Create the project directory
        val projectFolder = File(externalFilesTestDir, "${id}_$projectName")
        projectFolder.mkdir()

        // Create files in the directory
        val deletePhotoTimestamp: Long = 999999999
        val first = File(projectFolder, "${deletePhotoTimestamp}.jpg")
        first.createNewFile()
        val second = File(projectFolder, "11111111.jpg")
        second.createNewFile()
        val third = File(projectFolder, "videos")
        third.mkdir()
        val fourth = File(projectFolder, "tags.txt")
        fourth.createNewFile()

        val photoEntry = PhotoEntry(projectEntry.id, deletePhotoTimestamp)

        // When deleting photo
        FileUtils.deletePhoto(externalFilesTestDir, projectEntry, photoEntry)

        // Photo no longer exists
        assert(!first.exists())
        assert(second.exists())
        assert(third.exists())
    }

    @Test
    fun pathContainsReservedCharacter_noReservedCharacters_shouldPass() {
        // Given - a string without any reserved characters
        val testString = "1_Test Project Name"

        // When - the utility is called
        val containsReservedCharacters = FileUtils.pathContainsReservedCharacter(testString)

        // Then - the utility should indicate false, the path does not contain a reserved character
        assertFalse(containsReservedCharacters)
    }

    @Test
    fun pathContainsReservedCharacter_containsReservedCharacters_shouldFail() {
        // Given - a string with a reserved character
        val testString = "1_Test?Project Name/"

        // When - the utility is called
        val containsReservedCharacters = FileUtils.pathContainsReservedCharacter(testString)

        // Then - the utility should indicate true
        assertTrue(containsReservedCharacters)
    }

    @Test
    fun getPhotoUrlForPhotoEntryFromProject(){
        // Given - a created photo file, a photo entry and project abstraction
        // Create the project to test
        val projectName = "get photo url test project"
        val id: Long = 5
        // Create the project directory
        val projectFolder = File(externalFilesTestDir, "${id}_$projectName")
        projectFolder.mkdir()

        // Create files in the directory
        val timestamp: Long = 999999999
        val first = File(projectFolder, "${timestamp}.jpg")
        first.createNewFile()

        val photoEntry = PhotoEntry(id, id, timestamp)
        val project = Project(id, projectName, 0, 0, id, timestamp)

        // When - utilities gets the entry
        val photoUrl = FileUtils.getPhotoUrl(externalFilesTestDir, project, photoEntry)

        // Then - the returned path should be the same as the created path
        assert(photoUrl == first.absolutePath)
    }

    @Test
    fun getPhotoUrlForPhotoEntryFromProjectEntry(){
        // Given - a created photo file, a photo entry and project abstraction
        // Create the project to test
        val projectName = "get photo url test project"
        val id: Long = 6
        val projectEntry = ProjectEntry(id, projectName)

        // Create the project directory
        val projectFolder = File(externalFilesTestDir, "${id}_$projectName")
        projectFolder.mkdir()

        // Create files in the directory
        val timestamp: Long = 999999999
        val first = File(projectFolder, "${timestamp}.jpg")
        first.createNewFile()

        val photoEntry = PhotoEntry(id, id, timestamp)

        // When - utilities gets the entry
        val photoUrl = FileUtils.getPhotoUrl(externalFilesTestDir, projectEntry, photoEntry)

        // Then - the returned path should be the same as the created path
        assert(photoUrl == first.absolutePath)
    }

    @Test
    fun getCoverPhotoUrl(){
        // Given a project with a cover photo
        // Create the project to test
        val projectName = "cover photo url test project"
        val id: Long = 7
        // Create the project directory
        val projectFolder = File(externalFilesTestDir, "${id}_$projectName")
        projectFolder.mkdir()

        // Create files in the directory
        val timestamp: Long = 999999999
        val first = File(projectFolder, "${timestamp}.jpg")
        first.createNewFile()

        val project = Project(id, projectName, 0, 0, id, timestamp)

        // When - utility is called
        val url = FileUtils.getCoverPhotoUrl(externalFilesTestDir, project)

        // Then - url matches the created file url
        assert(url == first.absolutePath)
    }

    @Test
    fun getPhotoFileNameFromEntry() {
        // Given - a photo file
        val timestamp: Long = 12345
        val photoEntry = PhotoEntry(1, timestamp)

        // When
        val filenames = FileUtils.getPhotoFileNames(photoEntry)

        // Then
        assert(filenames[0] == "$timestamp.jpg")
        assert(filenames[1] == "$timestamp.png")
        assert(filenames[2] == "$timestamp.jpeg")
    }

    @Test
    fun getPhotoFileNameFromTimeStamp() {
        // Given
        val timestamp: Long = 12345

        // When
        val filenames = FileUtils.getPhotoFileNames(timestamp)

        // Then
        assert(filenames[0] == "$timestamp.jpg")
        assert(filenames[1] == "$timestamp.png")
        assert(filenames[2] == "$timestamp.jpeg")
    }

    companion object {
        private val TAG = FileUtilsTest::class.java.simpleName
    }
}