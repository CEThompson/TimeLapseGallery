package com.vwoom.timelapsegallery.gif

import android.content.Context
import android.os.Environment
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.di.InjectorUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import kotlin.coroutines.CoroutineContext

class GifWorker(context: Context, params: WorkerParameters)
    : Worker(context, params), CoroutineScope {

    private lateinit var projectRepository: ProjectRepository

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

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
            val gifExists: Boolean = (gif != null)
            if (!gifExists) continue // ignore all time-lapses the user is not interested in

            val projectChanged: Boolean = (project.project_updated == 1)
            // Update GIF if the project has been updated (picture added)
            if (projectChanged) {
                GifUtils.updateGif(externalFilesDir, project)
                launch {
                    projectRepository.markProjectUnchanged(project)
                }
            }
        }

        return Result.success()
    }

    companion object {
        val TAG = GifWorker::class.java.simpleName
    }

}