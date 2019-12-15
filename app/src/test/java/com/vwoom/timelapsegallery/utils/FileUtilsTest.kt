package com.vwoom.timelapsegallery.utils


import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileUtilsTest {

    // TODO naming
    // TODO Given/when/then
    // TODO assertion framework

    @Rule @JvmField
    val folder = TemporaryFolder()

    /* Tests retrieving a list of photo entries from a project folder*/
    @Test
    fun getPhotoEntriesInProjectDirectory_directoryWithImagesTextAndFolder_twoOrderedPhotoEntries() {
        /* Given */
        // Create the project to test
        val projectName = "test project"
        val projectEntry = ProjectEntry(1, projectName, 0)
        // Create the base folder for the pictures
        val picturesFolder = folder.newFolder("pictures")
        // Create the project directory
        val projectFolder = File(picturesFolder, "1_$projectName")
        projectFolder.mkdir()
        // Create a set of image files to test: note the ordering on these
        var file = File(projectFolder, "99999999.jpg")
        file.createNewFile()
        file = File(projectFolder, "11111111.jpg")
        file.createNewFile()
        // Also create folder to contain encoded videos
        file = File(projectFolder, "videos")
        file.mkdir()
        // And txt to contain tags
        file = File(projectFolder, "tags.txt")
        file.createNewFile()

        /* Actually use the method we are testing with mocked data */
        /* When */
        val listOfPhotoEntries = FileUtils.getPhotoEntriesInProjectDirectory(picturesFolder, projectEntry)

        val expectedList = listOf(
                PhotoEntry(1,11111111),
                PhotoEntry(1,99999999))

        /* Then */
        /* Assert out response is equal to the expectation */
        assertThat(listOfPhotoEntries, `is`(expectedList))

        // TODO convert to logs?
        //System.out.println("$TAG $projectFolder")
        //System.out.println("$TAG ${listOfPhotoEntries == null}")
        //System.out.println("$TAG returned list is $listOfPhotoEntries")
        //System.out.println("$TAG test list is $assertionList")
    }

    @Test
    fun createTemporaryImageFile_tempImageFolder_tempFileShouldExist() {
        /* Given */
        val picturesFolder = folder.newFolder("pictures")
        // Create the project directory
        val tempFolder = File(picturesFolder, FileUtils.TEMP_FILE_SUBDIRECTORY)
        tempFolder.mkdir()

        /* When */
        val fileCreated = FileUtils.createTemporaryImageFile(tempFolder)

        System.out.println("$TAG $fileCreated")
        assert(fileCreated != null)
    }

    @Test
    fun createFinalFileFromTempTest_shouldPass() {
        // Create the pictures directory and temporary_images directory
        val picturesFolder = folder.newFolder("pictures")
        val tempFolder = File(picturesFolder, FileUtils.TEMP_FILE_SUBDIRECTORY)
        tempFolder.mkdir()

        // Create the temporary image file
        val tempFile = FileUtils.createTemporaryImageFile(tempFolder)
        val timestamp = System.currentTimeMillis()

        // Create the project
        val projectEntry = ProjectEntry("test project", 0)

        // Create the folder for the project
        val finalFile = FileUtils.createFinalFileFromTemp(picturesFolder, tempFile.absolutePath, projectEntry, timestamp)

        assert(!tempFile.exists()) // make sure temp file was deleted
        assert(finalFile != null) // make sure final file was created
        assert(finalFile.exists())
        // TODO make other assertions about the final copied file?

        // TODO convert to logs?
        System.out.println("$TAG tempFile path $finalFile")
        System.out.println("$TAG finalFile path $tempFile")
        System.out.println("$TAG tempFile exists ${tempFile.exists()}")
        System.out.println("$TAG finalFile exists ${finalFile.exists()}")

    }

    @Test
    fun renameProject() {
        /* Given - A named project that has files */
        /* When - We run the function renameProject */
        /* Then - Expect the previous folder to be gone, a new folder with the same files to exist */
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
    }

    @Test
    fun deletePhoto1() {
    }

    @Test
    fun pathContainsReservedCharacter() {
    }

    companion object {
        private val TAG = FileUtilsTest::class.java.simpleName
    }
}