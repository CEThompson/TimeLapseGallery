package com.vwoom.timelapsegallery.gif

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import java.io.File
import javax.inject.Inject

class GifWorker(context: Context, params: WorkerParameters)
    : Worker(context, params) {

    @Inject
    lateinit var projectRepository: ProjectRepository

    override fun doWork(): Result {
        Log.d(TAG, "Gif Worker Tracker: Executing work")

        // Only update gifs if preference is enabled
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
            Log.d(TAG, "Gif Worker Tracker: Unable to get external files dir. Reason: ${e.message}")
            return Result.failure()
        }

        // Loop through projects and update projects which have gifs
        val projects = projectRepository.getAllProjects()
        for (project in projects) {
            val gif = GifUtils.getGifForProject(externalFilesDir, project)
            if (gif != null) {
                // TODO: in the future figure out how to detect if a gif is already updated to the picture set
                GifUtils.updateGif(externalFilesDir, project)
            }
        }

        return Result.success()
    }

    companion object {
        const val TAG = "gif_worker"
    }
}