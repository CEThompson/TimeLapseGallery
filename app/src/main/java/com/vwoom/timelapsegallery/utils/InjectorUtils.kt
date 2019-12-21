package com.vwoom.timelapsegallery.utils

import android.content.Context
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.details.CameraViewModelFactory

object InjectorUtils {

    private fun getRepository(context: Context): Repository {
        return Repository.getInstance(
                TimeLapseDatabase.getInstance(context.applicationContext))
    }

    fun provideCameraViewModelFactory(context: Context, projectId: Long?): CameraViewModelFactory {
        val repository = getRepository(context)
        return CameraViewModelFactory(repository, projectId)
    }

}