package com.vwoom.timelapsegallery.gif

import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GifUtilsFfmpegTest {

    @Rule
    @JvmField
    val testFolder = TemporaryFolder()

    private lateinit var externalFilesTestDir: File

    @Before
    fun setUp(){
        externalFilesTestDir = testFolder.newFolder("pictures")
    }

    // This test ensures that a project with a set of images can convert to a GIF
    // Note: This seems to require instrumentation to run ffmpeg commands
    @Test
    fun makeGif_createsGifFromProject(){
        // Given - a project with a set of images
        val project = ProjectEntry(1, null)
        val projectFolder = ProjectUtils.getProjectFolder(externalFilesTestDir, project)
        if (!projectFolder.exists()) projectFolder.mkdir()
        for (i in 0..6) {
            val photoFile = File(projectFolder, "${i}.jpeg")
            photoFile.mkdir()
        }

        // When - we call the function to actually make the GIF
        GifUtils.makeGif(externalFilesTestDir, project)

        // Then a gif for the project should exist
        val gif = GifUtils.getGifForProject(externalFilesTestDir, project)
        assert (gif!= null)
    }

    @Test
    fun updateGif() {
        // Given a gif for a project
        val project = ProjectEntry(1000, null)
        val gifDir = File(externalFilesTestDir, FileUtils.GIF_FILE_SUBDIRECTORY)
        if (!gifDir.exists()) gifDir.mkdir()
        val makeGif = File(gifDir, "${project.id}.gif")
        makeGif.mkdir()

        val firstGif = GifUtils.getGifForProject(externalFilesTestDir, project)
        assert(firstGif!=null)
        if (firstGif == null) return

        // When we update the gif
        GifUtils.updateGif(externalFilesTestDir, project)

        // It should not be the same file
        val updatedGif = GifUtils.getGifForProject(externalFilesTestDir, project)
        assert(firstGif != updatedGif)
    }
}