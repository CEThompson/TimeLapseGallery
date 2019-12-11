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
    fun getPhotosInDirectoryTest_() {
        val projectName = "test project"
        val projectEntry = ProjectEntry(1, projectName, 0)

        // Create the folder for the project
        val projectFolder = folder.newFolder("1_$projectName")

        // Create files to test
        var file = File(projectFolder, "1.jpg")
        file.createNewFile()

        file = File(projectFolder, "2.jpg")
        file.createNewFile()

        val listOfPhotoEntries = FileUtils.getPhotosInDirectory(projectFolder, projectEntry)
        assert(listOfPhotoEntries.equals(
                listOf(
                PhotoEntry(1,1,1),
                PhotoEntry(2,1,2))
        ))
    }

    @Test
    fun createTemporaryImageFile() {
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
}