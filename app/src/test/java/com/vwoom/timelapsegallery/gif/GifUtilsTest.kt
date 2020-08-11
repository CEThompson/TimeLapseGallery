package com.vwoom.timelapsegallery.gif

import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.utils.FileUtils
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GifUtilsTest {

    @Rule
    @JvmField
    val testFolder = TemporaryFolder()

    private lateinit var externalFilesTestDir: File

    @Before
    fun setUp() {
        externalFilesTestDir = testFolder.newFolder("pictures")
    }

    // Makes sure a project with a gif does return a gif for the project
    @Test
    fun `getGifForProject() when project GIF exists`() {
        // 1. Given a project and a created GIF (faked .gif file)
        val project = ProjectEntry(1000, null)
        val gifDir = File(externalFilesTestDir, FileUtils.GIF_FILE_SUBDIRECTORY)
        if (!gifDir.exists()) gifDir.mkdir()
        val makeGif = File(gifDir, "${project.id}.gif")
        makeGif.mkdir()
        // 2. When we get the gif for the project
        val gif = GifUtils.getGifForProject(externalFilesTestDir, project)
        // 3. It exists
        assertTrue(gif != null)
        if (gif != null) assert(gif.exists())
    }

    // Makes sure a project with no gif does not return a gif for the project
    @Test
    fun `getGifForProject() when no GIF exists`() {
        // 1. Given a gif and a project
        val project = ProjectEntry(1000, null)
        // 2. When we get the gif for the project
        val gif = GifUtils.getGifForProject(externalFilesTestDir, project)
        // It does not exist
        assertTrue(gif == null)
    }

    @Test
    fun `deleteGif() then no GIF for project exists`() {
        // Given a gif for a project
        val project = ProjectEntry(1000, null)
        val gifDir = File(externalFilesTestDir, FileUtils.GIF_FILE_SUBDIRECTORY)
        if (!gifDir.exists()) gifDir.mkdir()
        val makeGif = File(gifDir, "${project.id}.gif")
        makeGif.mkdir()

        // When we call delete gif
        GifUtils.deleteGif(externalFilesTestDir, project)

        // Then no GIF for that project should exist
        val gif = GifUtils.getGifForProject(externalFilesTestDir, project)
        assertTrue(gif == null)
    }


    // TODO: figure out how to test GIF worker
    /*@Test
    fun scheduleGifWorker() {
    }
    @Test
    fun cancelGifWorker() {
    }*/

}