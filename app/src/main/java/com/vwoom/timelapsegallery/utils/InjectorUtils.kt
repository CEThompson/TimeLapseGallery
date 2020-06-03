package com.vwoom.timelapsegallery.utils

import android.content.Context
import com.vwoom.timelapsegallery.camera2.Camera2ViewModelFactory
import com.vwoom.timelapsegallery.cameraX.CameraXViewModelFactory
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.datasource.WeatherLocalDataSource
import com.vwoom.timelapsegallery.data.datasource.WeatherRemoteDataSource
import com.vwoom.timelapsegallery.data.repository.*
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.detail.DetailViewModelFactory
//import com.vwoom.timelapsegallery.gallery.GalleryViewModelFactory
import com.vwoom.timelapsegallery.settings.SettingsViewModelFactory

object InjectorUtils {

    fun getProjectRepository(context: Context): ProjectRepository {
        return ProjectRepository.getInstance(
                TimeLapseDatabase.getInstance(context.applicationContext).projectDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).photoDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).coverPhotoDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).projectScheduleDao())
    }

    private fun getTagRepository(context: Context): TagRepository {
        return TagRepository.getInstance(
                TimeLapseDatabase.getInstance(context.applicationContext).projectTagDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).tagDao())
    }

    private fun getWeatherRepository(context: Context): WeatherRepository {
        val weatherDao = TimeLapseDatabase.getInstance(context.applicationContext).weatherDao()
        val localDataSource = WeatherLocalDataSource(weatherDao)
        val remoteDataSource = WeatherRemoteDataSource()
        return WeatherRepository(localDataSource, remoteDataSource)
    }

    fun provideCamera2ViewModelFactory(context: Context, photo: Photo?, projectView: ProjectView?): Camera2ViewModelFactory {
        val projectRepository = getProjectRepository(context)
        return Camera2ViewModelFactory(
                projectRepository,
                photo,
                projectView)
    }

    fun provideCameraXViewModelFactory(context: Context, photo: Photo?, projectView: ProjectView?): CameraXViewModelFactory {
        val projectRepository = getProjectRepository(context)
        return CameraXViewModelFactory(
                projectRepository,
                photo,
                projectView)
    }

    fun provideDetailsViewModelFactory(context: Context, projectView: ProjectView): DetailViewModelFactory {
        val projectRepository = getProjectRepository(context)
        val tagRepository = getTagRepository(context)
        return DetailViewModelFactory(
                projectRepository,
                tagRepository,
                projectView.project_id)
    }

    /*fun provideGalleryViewModelFactory(context: Context): GalleryViewModelFactory {
        val projectRepository = getProjectRepository(context)
        val tagRepository = getTagRepository(context)
        val weatherRepository = getWeatherRepository(context)
        return GalleryViewModelFactory(
                projectRepository,
                tagRepository,
                weatherRepository)
    }*/

    fun provideSettingsViewModelFactory(): SettingsViewModelFactory {
        return SettingsViewModelFactory()
    }

}