package com.vwoom.timelapsegallery.utils

import android.content.Context
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.details.CameraViewModelFactory
import com.vwoom.timelapsegallery.gallery.GalleryViewModel

object InjectorUtils {

    private fun getRepository(context: Context): Repository {
        return Repository.getInstance(
                TimeLapseDatabase.getInstance(context.applicationContext).projectDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).photoDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).coverPhotoDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).projectScheduleDao())
    }

    fun provideCameraViewModelFactory(context: Context, project: Project?): CameraViewModelFactory {
        val repository = getRepository(context)
        return CameraViewModelFactory(repository, project)
    }

    fun provideGalleryViewModel(context: Context): GalleryViewModel {
        val repository = getRepository(context)
        return GalleryViewModel(repository)
    }

}