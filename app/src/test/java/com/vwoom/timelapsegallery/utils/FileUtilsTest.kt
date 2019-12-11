package com.vwoom.timelapsegallery.utils


import com.vwoom.timelapsegallery.database.entry.PhotoEntry
import com.vwoom.timelapsegallery.database.entry.ProjectEntry
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileUtilsTest {

    @Rule @JvmField
    val folder = TemporaryFolder()

    /* Tests retrieving a list of photo entries from a project folder*/
    @Test
    fun getPhotoEntriesInProjectDirectory_shouldPass() {
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
        val listOfPhotoEntries = FileUtils.getPhotoEntriesInProjectDirectory(picturesFolder, projectEntry)

        val assertionList = listOf(
                PhotoEntry(1,11111111),
                PhotoEntry(1,99999999))

        /* Assert out response is equal to the expectation */
        assert(listOfPhotoEntries.equals(
                assertionList))

        // TODO convert to logs?
        //System.out.println("$TAG $projectFolder")
        //System.out.println("$TAG ${listOfPhotoEntries == null}")
        //System.out.println("$TAG returned list is $listOfPhotoEntries")
        //System.out.println("$TAG test list is $assertionList")
    }

    @Test
    fun createTemporaryImageFileTest_shouldPass() {

        val picturesFolder = folder.newFolder("pictures")

        // Create the project directory
        val tempFolder = File(picturesFolder, FileUtils.TEMP_FILE_SUBDIRECTORY)
        tempFolder.mkdir()

        val fileCreated = FileUtils.createTemporaryImageFile(tempFolder)

        System.out.println("$TAG $fileCreated")
        assert(fileCreated != null)
    }

    @Test
    fun createFinalFileFromTemp() {
    }

    @Test
    fun renameProject() {
    }

    @Test
    fun deleteTempFiles() {
    }

    @Test
    fun deleteProject() {
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