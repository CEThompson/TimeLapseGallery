package com.vwoom.timelapsegallery.utils

import android.content.Context
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.details.CameraViewModelFactory
import com.vwoom.timelapsegallery.gallery.GalleryViewModel

object InjectorUtils {

    private fun getRepository(context: Context): Repository {
        return Repository.getInstance(
                TimeLapseDatabase.getInstance(context.applicationContext).projectDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).photoDao())
    }

    fun provideCameraViewModelFactory(context: Context, photo: Photo?): CameraViewModelFactory {
        val repository = getRepository(context)
        return CameraViewModelFactory(repository, photo)
    }

    fun provideGalleryViewModel(context: Context): GalleryViewModel {
        val repository = getRepository(context)
        return GalleryViewModel(repository)
    }

}