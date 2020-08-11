package com.vwoom.timelapsegallery.gif

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.utils.InjectorUtils
import timber.log.Timber
import java.io.File

class GifWorker(context: Context, params: WorkerParameters)
    : Worker(context, params) {

    private lateinit var projectRepository: ProjectRepository

    override fun doWork(): Result {
        Timber.d("Gif Worker Tracker: Executing work")

        // Only update GIFs if preference is enabled
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val gifConvertEnabled = prefs.getBoolean(applicationContext.getString(R.string.key_gif_auto_convert), true)
        if (!gifConvertEnabled) {
            return Result.success()
        }

        // Try to get the external files directory. Return failure if unsuccessful
        val externalFilesDir: File
        try {
            externalFilesDir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        } catch (e: Exception) {
            Timber.d("Gif Worker Tracker: Unable to get external files dir. Reason: ${e.message}")
            return Result.failure()
        }

        // Loop through projects and update projects which have gifs
        projectRepository = InjectorUtils.getProjectRepository(applicationContext)
        val projects = projectRepository.getAllProjects()
        for (project in projects) {
            val gif = GifUtils.getGifForProject(externalFilesDir, project)
            if (gif != null) {
                // TODO (1.3): make GIF conversion more efficient. Figure out how to detect if a gif is already updated to the picture set
                GifUtils.updateGif(externalFilesDir, project)
            }
        }

        return Result.success()
    }
}