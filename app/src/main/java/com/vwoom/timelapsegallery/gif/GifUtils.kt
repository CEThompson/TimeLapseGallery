package com.vwoom.timelapsegallery.gif

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.arthenica.mobileffmpeg.FFmpeg
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.utils.FileUtils
import timber.log.Timber
import java.io.File

object GifUtils {
    private val TAG = GifUtils::class.java.simpleName

    private fun getGifDirectory(externalFilesDir: File): File {
        val gifDir = File(externalFilesDir, FileUtils.GIF_FILE_SUBDIRECTORY)
        if (!gifDir.exists()) gifDir.mkdir()
        return gifDir
    }


    // TODO (update 1.3): create control for framerate
    // TODO (update 1.3): create control for scale
    // Creates a .gif from the set of photos for a project
    fun makeGif(externalFilesDir: File, project: ProjectEntry, fps: Int = 14, scale: Int = 400) {
        // Write the list of paths for the files to a text file for use by ffmpeg
        val listTextFile = FileUtils.createTempListPhotoFiles(externalFilesDir, project)

        // Get the meta directory for the project
        val projectGifDir = getGifDirectory(externalFilesDir)

        // Define the output path for the gif
        val outputGif = "${projectGifDir.absolutePath}/${project.id}.gif"

        // Create the command for ffmpeg
        val ffmpegCommand = "-r $fps -y -f concat -safe 0 -i $listTextFile -vf scale=$scale:-1 $outputGif"
        FFmpeg.execute(ffmpegCommand)

        //Use this block for logging
        //val rc = FFmpeg.execute(ffmpegCommand)
        //val lastCommandOutput = Config.getLastCommandOutput()
        //Log.d("TLG.GIF:", "Creating list of text files")
        //Log.d("TLG.GIF:", "Output gif path is: $outputGif")
        //Log.d("TLG.GIF:", "Executing ffmpeg command: $ffmpegCommand")
        //Log.d("TLG.GIF:", "Executed, rc is: $rc")
        //Log.d("TLG.GIF:", "Last command output: $lastCommandOutput")
    }

    fun getGifForProject(externalFilesDir: File, project: ProjectEntry): File? {
        val projectGifDir = getGifDirectory(externalFilesDir)
        val gifFile = File("${projectGifDir.absolutePath}/${project.id}.gif")
        return if (gifFile.exists()) gifFile
        else null
    }

    fun deleteGif(externalFilesDir: File, project: ProjectEntry) {
        val curGif = getGifForProject(externalFilesDir, project)
        if (curGif != null) FileUtils.deleteRecursive(curGif)
    }

    fun scheduleGifWorker(context: Context) {
        Timber.d("Gif Util Tracker: Creating and enqueuing gif work request")
        val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(true)
                .setRequiresStorageNotLow(true)

        // Set device idle for api 23+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            constraints.setRequiresDeviceIdle(true)

        val scheduleGifConvertRequest = OneTimeWorkRequest.Builder(GifWorker::class.java)
                .setConstraints(constraints.build())
                .addTag(GifWorker.TAG)
                .build()

        // Schedule the work, if it already exists keep the already scheduled work
        WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, scheduleGifConvertRequest)
    }

    /* Cancels alarms AND cancels any notification workers*/
    fun cancelGifWorker(context: Context) {
        Timber.d("Gif Util Tracker: Canceling gif worker")
        WorkManager.getInstance(context).cancelAllWorkByTag(GifWorker.TAG)
    }

    fun updateGif(externalFilesDir: File, project: ProjectEntry) {
        // Delete the old gif then write the gif from the picture set
        deleteGif(externalFilesDir, project)
        makeGif(externalFilesDir, project)
    }
}