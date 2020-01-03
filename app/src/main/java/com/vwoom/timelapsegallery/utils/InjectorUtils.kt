package com.vwoom.timelapsegallery.utils

import android.content.Context
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.detail.CameraViewModelFactory
import com.vwoom.timelapsegallery.detail.DetailViewModelFactory
import com.vwoom.timelapsegallery.detail.GalleryViewModelFactory
import com.vwoom.timelapsegallery.gallery.GalleryViewModel
import com.vwoom.timelapsegallery.settings.SettingsViewModel

object InjectorUtils {

    private fun getRepository(context: Context): Repository {
        return Repository.getInstance(
                TimeLapseDatabase.getInstance(context.applicationContext).projectDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).photoDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).coverPhotoDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).projectScheduleDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).projectTagDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).tagDao()
                )
    }

    fun provideCameraViewModelFactory(context: Context, photo: Photo?, project: Project?): CameraViewModelFactory {
        val repository = getRepository(context)
        return CameraViewModelFactory(repository, photo, project)
    }

    fun provideDetailsViewModelFactory(context: Context, project: Project): DetailViewModelFactory {
        val repository = getRepository(context)
        return DetailViewModelFactory(repository, project.project_id)
    }

    fun provideGalleryViewModelFactory(context: Context): GalleryViewModelFactory {
        val repository = getRepository(context)
        return GalleryViewModelFactory(repository)
    }

    fun provideSettingsViewModel(context: Context): SettingsViewModel {
        val repository = getRepository(context)
        return SettingsViewModel(repository)
    }

}