package com.vwoom.timelapsegallery.utils


import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
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

    private lateinit var picturesFolder: File

    @Before
    fun setUp(){
        picturesFolder = testFolder.newFolder("pictures")
    }

    /* Tests retrieving a list of photo entries from a project folder*/
    @Test
    fun getPhotoEntriesInProjectDirectory_directoryWithImagesTextAndFolder_twoOrderedPhotoEntries() {
        /* Given */
        // Create the project to test
        val projectName = "test project"
        val projectEntry = ProjectEntry(1, projectName)

        // Create the project directory
        val projectFolder = File(picturesFolder, "1_$projectName")
        projectFolder.mkdir()

        // Create files in the directory
        File(projectFolder, "99999999.jpg").createNewFile()
        File(projectFolder, "11111111.jpg").createNewFile()
        File(projectFolder, "videos").mkdir()
        File(projectFolder, "tags.txt").createNewFile()

        /* Actually use the method we are testing with mocked data */
        /* When */
        val listOfPhotoEntries = FileUtils.getPhotoEntriesInProjectDirectory(picturesFolder, projectEntry)

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
        val tempFolder = File(picturesFolder, FileUtils.TEMP_FILE_SUBDIRECTORY)
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
        val tempFolder = File(picturesFolder, FileUtils.TEMP_FILE_SUBDIRECTORY)
        tempFolder.mkdir()

        // Create the temporary image file
        val tempFile = FileUtils.createTemporaryImageFile(tempFolder)
        val timestamp = System.currentTimeMillis()

        // Create the project
        val projectEntry = ProjectEntry("test project")

        // Create the folder for the project
        val finalFile = FileUtils.createFinalFileFromTemp(picturesFolder, tempFile.absolutePath, projectEntry, timestamp)

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
        val projectFolder = File(picturesFolder, "${id}_$projectName")
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
        FileUtils.renameProject(picturesFolder, projectEntry, projectEntryToRename)

        /* Then - Expect the previous folder to be gone, a new folder with the same files to exist */
        assert(File(picturesFolder, "${id}_$projectRenameString").exists())
    }

    @Test
    fun deleteTempFiles() {
        /* Given - A directory of temp files */

        /* When - deleteTempFiles() is run */

        /* Then - the directory is empty / gone */
    }

    @Test
    fun deleteProject() {
        /* Given an existing project */

        /* When deleteProject() is run */

        /* Project folder no longer exists */
    }

    @Test
    fun deletePhoto() {
        // Given - a photo belonging to a project

        // When deleting photo


        // Photo no longer exists
    }

    @Test
    fun pathContainsReservedCharacter_noReservedCharacters_shouldPass() {
        // Given
        val testString = "1_Test Project Name"

        // When
        val containsReservedCharacters = FileUtils.pathContainsReservedCharacter(testString)

        // Then
        assertFalse(containsReservedCharacters)
    }

    @Test
    fun pathContainsReservedCharacter_containsReservedCharacters_shouldFail() {
        // Given
        val testString = "1_Test?Project Name/"

        // When
        val containsReservedCharacters = FileUtils.pathContainsReservedCharacter(testString)

        // Then
        assertTrue(containsReservedCharacters)
    }

    @Test
    fun getPhotoUrlFromProject(){

    }

    @Test
    fun getPhotoUrlFromProjectEntry(){

    }

    @Test
    fun getCoverPhotoUrl(){

    }

    @Test
    fun getPhotoFileNameFromEntry() {
    }

    @Test
    fun getPhotoFileNameFromTimeStamp() {

    }

    @After
    fun cleanUp(){
        testFolder.delete()
    }

    companion object {
        private val TAG = FileUtilsTest::class.java.simpleName
    }
}