package com.vwoom.timelapsegallery.utils

import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FfmpegTest {

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
        ProjectUtils.makeGif(externalFilesTestDir, project)

        // Then a gif for the project should exist
        val gif = ProjectUtils.getGifForProject(externalFilesTestDir, project)
        assert (gif!= null)
    }
}