package com.vwoom.timelapsegallery.utils

import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.view.ProjectView

import com.vwoom.timelapsegallery.utils.ProjectUtils.getPhotoEntriesInProjectDirectory

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.*

class ProjectUtilsTest {

    @Rule @JvmField
    val testFolder = TemporaryFolder()

    private lateinit var externalFilesTestDir: File

    @Before
    fun setUp(){
        externalFilesTestDir = testFolder.newFolder("pictures")
    }

    // Tests retrieving a list of photo entries from a project folder
    @Test
    fun getPhotoEntriesInProjectDirectory_directoryWithData_returnsTwoOrderedPhotoEntries() {
        // Given:
        // a project to test
        val projectName = "test project"
        val projectEntry = ProjectEntry(1, projectName)
        // a project directory
        val projectFolder = ProjectUtils.getProjectFolder(externalFilesTestDir, projectEntry)
        projectFolder.mkdirs()
        println(projectFolder.absolutePath)
        println(projectFolder.exists())
        // and files in the directory
        File(projectFolder, "99999999.jpg").createNewFile()
        File(projectFolder, "11111111.jpg").createNewFile()
        val metaFolder = File(projectFolder, "meta")
        metaFolder.mkdir()
        File(metaFolder, "tags.txt").createNewFile()

        // When we get the photo entries from the method
        val listOfPhotoEntries = getPhotoEntriesInProjectDirectory(externalFilesTestDir, projectEntry)

        // Then assert that the list is as expected
        val expectedList = listOf(
                PhotoEntry(1,11111111),
                PhotoEntry(1,99999999))
        MatcherAssert.assertThat(listOfPhotoEntries, CoreMatchers.`is`(expectedList))
    }

    @Test
    fun renameProject() {
        // Given
        // a project to test
        val projectName = "second test project"
        val id: Long = 2
        val projectEntry = ProjectEntry(id, projectName)
        // a project directory
        val projectFolder = ProjectUtils.getProjectFolder(externalFilesTestDir, projectEntry)
        projectFolder.mkdirs()
        // files in the directory
        val children = listOf("99999999.jpg", "11111111.jpg")
        for (child in children) File(projectFolder, child).createNewFile()

        // and project entry to rename it to
        val projectRenameString = "test rename"
        val projectEntryToRename = ProjectEntry(id, projectRenameString)

        // When we run the function renameProject
        ProjectUtils.renameProject(externalFilesTestDir, projectEntry, projectEntryToRename)

        // Then we expect the previous folder to be gone
        assertTrue(!projectFolder.exists())
        // and the renamed folder to exist
        val renamedProject = ProjectUtils.getProjectFolder(externalFilesTestDir, projectEntryToRename)
        assertTrue(renamedProject.exists())
        // with the same children
        for (child in children) assertTrue(File(renamedProject, child).exists())
    }

    @Test
    fun deleteProject() {
        // Given
        // a project to test
        val projectName = "delete test project"
        val id: Long = 3
        val projectEntry = ProjectEntry(id, projectName)
        // a project directory
        val projectFolder = ProjectUtils.getProjectFolder(externalFilesTestDir, projectEntry)
        projectFolder.mkdirs()
        // files in the directory
        val first = File(projectFolder, "99999999.jpg")
        first.createNewFile()
        val second = File(projectFolder, "11111111.jpg")
        second.createNewFile()

        // When deleteProject() is run
        ProjectUtils.deleteProject(externalFilesTestDir, projectEntry)

        // Then the project folder and its children no longer exist
        assertTrue(!first.exists())
        assertTrue(!second.exists())
        assertTrue(!projectFolder.exists())

        // Also its meta dir should not exist

        val metaDir = FileUtils.getMetaSubdirectory(externalFilesTestDir)
        val projectMetaSubfolder = File(metaDir, projectEntry.id.toString())
        assertTrue(!projectMetaSubfolder.exists())

    }

    @Test
    fun deletePhoto() {
        // Given a project to test
        val projectName = "delete photo test project"
        val id: Long = 4
        val projectEntry = ProjectEntry(id, projectName)
        // a project directory
        val projectFolder = ProjectUtils.getProjectFolder(externalFilesTestDir, projectEntry)
        projectFolder.mkdirs()
        // two photos in the directory
        val firstPhotoTimestamp: Long = 999999999
        val first = File(projectFolder, "${firstPhotoTimestamp}.jpg")
        println(projectFolder.absolutePath)
        println(first.absolutePath)
        first.createNewFile()
        val second = File(projectFolder, "11111111.jpg")
        second.createNewFile()
        // and a photo entry derived for the first photo
        val firstPhotoEntry = PhotoEntry(projectEntry.id, firstPhotoTimestamp)

        // When deleting the photo
        ProjectUtils.deleteProjectPhoto(externalFilesTestDir, projectEntry, firstPhotoEntry)

        // Then first photo no longer exists
        assertTrue(!first.exists())
        // but the second does exist
        assertTrue(second.exists())
    }

    @Test
    fun getPhotoUrl(){
        // Given a project to test
        val projectName = "get photo url test project"
        val id: Long = 5
        val projectEntry = ProjectEntry(id, projectName)

        // a project directory
        val projectFolder = ProjectUtils.getProjectFolder(externalFilesTestDir, projectEntry)
        projectFolder.mkdirs()

        // a photo in the directory
        val timestamp: Long = 999999999
        val first = File(projectFolder, "${timestamp}.jpg")
        first.createNewFile()
        // and entries derived for photo and project
        val photoEntry = PhotoEntry(id, id, timestamp, null, null, null, null)

        // When the utility gets the entry
        val photoUrl = ProjectUtils.getProjectPhotoUrl(externalFilesTestDir, projectEntry, photoEntry.timestamp)

        // Then the returned path should be the same as the created path
        assertTrue(photoUrl == first.absolutePath)
    }

    @Test
    fun getMetaDirectoryForProject() {
        // given a project
        val project = ProjectEntry(5, "test name")

        // when we get the directory for the project
        val metaDir = ProjectUtils.getMetaDirectoryForProject(externalFilesTestDir, project.id)
        // and get the relative path
        val absPath = metaDir.absolutePath
        val relPath = absPath.substring(metaDir.absolutePath.lastIndexOf(File.separatorChar)+1)

        // Then the meta directory exists
        assertTrue(metaDir.exists())
        // the relative path is equal to the project ID (ex. "meta/5")
        assertTrue(relPath == project.id.toString())
    }

    // NOTE: This test will likely fail if run at the very beginning of a new year
    @Test
    fun isProjectDueToday() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        val twoDaysAgo = calendar[Calendar.DAY_OF_YEAR] - 2

        calendar[Calendar.DAY_OF_YEAR] = twoDaysAgo

        val timestampTwoDaysAgo = calendar.timeInMillis
        val project = ProjectView(1, null, 2, 1, timestampTwoDaysAgo)

        // When
        val isProjectDueToday = ProjectUtils.isProjectDueToday(project)

        // Then
        assertTrue(isProjectDueToday)
    }

    @Test
    fun isProjectDueTomorrow() {
        // Given
        val project = ProjectView(1, null, 1, 1, System.currentTimeMillis())
        // When
        val isProjectDueTomorrow = ProjectUtils.isProjectDueTomorrow(project)
        // Then
        assertTrue(isProjectDueTomorrow)
    }


}