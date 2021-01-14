package com.vwoom.timelapsegallery.gif

import android.graphics.Bitmap
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@RunWith(AndroidJUnit4ClassRunner::class)
class GifUtilsFfmpegTest {

    @Rule
    @JvmField
    val testFolder = TemporaryFolder()

    private lateinit var externalFilesTestDir: File

    @Before
    fun setUp() {
        externalFilesTestDir = testFolder.newFolder("pictures")
    }

    // This test ensures that a project with a set of images converts to a GIF
    // Note: This seems to require instrumentation to run ffmpeg commands
    @Test
    fun makeGif_createsGifFromProject() {
        // Given - a project with a set of images
        val project = ProjectEntry(1, null)
        val projectFolder = ProjectUtils.getProjectFolder(externalFilesTestDir, project)
        if (!projectFolder.exists()) projectFolder.mkdirs()

        // Create a few empty bitmaps written to jpeg files
        val bm = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        for (i in 0..3) {
            val filename = "${i}.jpeg"
            saveBitmapToFile(projectFolder, filename, bm)
        }

        // When - we call the function to actually make the GIF
        GifUtils.makeGif(externalFilesTestDir, project)

        // Then a gif for the project should exist
        val gif = GifUtils.getGifForProject(externalFilesTestDir, project)
        assertTrue(gif != null)
    }

    // This test ensures that an updated gif is not its previous version
    @Test
    fun updateGif() {
        // Given a gif for a project
        val project = ProjectEntry(1000, null)
        val gifDir = File(externalFilesTestDir, FileUtils.GIF_FILE_SUBDIRECTORY)
        if (!gifDir.exists()) gifDir.mkdir()
        val makeGif = File(gifDir, "${project.id}.gif")
        makeGif.mkdir()

        val firstGif = GifUtils.getGifForProject(externalFilesTestDir, project)
        assertTrue(firstGif != null)
        if (firstGif == null) return

        // When we update the gif
        GifUtils.updateGif(externalFilesTestDir, project)

        // It should not be the same file
        val updatedGif = GifUtils.getGifForProject(externalFilesTestDir, project)
        assertTrue(firstGif != updatedGif)
    }

    // A helper function for creating an image to make GIFs out of
    private fun saveBitmapToFile(
            dir: File,
            filename: String,
            bm: Bitmap,
            format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
            quality: Int = 50) {
        val imageFile = File(dir, filename)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(imageFile)
            bm.compress(format, quality, fos)
            fos.close()
        } catch (e: IOException) {
            fos?.close()
        }
    }

}