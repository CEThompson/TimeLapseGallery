package com.vwoom.timelapsegallery.utils

import android.content.Context
import android.content.Intent
import com.vwoom.timelapsegallery.camera2.Camera2ViewModelFactory
import com.vwoom.timelapsegallery.cameraX.CameraXViewModelFactory
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.repository.*
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.detail.DetailViewModelFactory
import com.vwoom.timelapsegallery.detail.GalleryViewModelFactory
import com.vwoom.timelapsegallery.settings.SettingsViewModelFactory
import com.vwoom.timelapsegallery.widget.WidgetGridRemoteViewsFactory

object InjectorUtils {

    private fun getCoverPhotoRepository(context: Context): CoverPhotoRepository {
        return CoverPhotoRepository.getInstance(
                TimeLapseDatabase.getInstance(context.applicationContext).coverPhotoDao())
    }

    private fun getPhotoRepository(context: Context): PhotoRepository {
        return PhotoRepository.getInstance(
                TimeLapseDatabase.getInstance(context.applicationContext).photoDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).projectDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).coverPhotoDao())
    }

    fun getProjectRepository(context: Context): ProjectRepository {
        return ProjectRepository.getInstance(
                TimeLapseDatabase.getInstance(context.applicationContext).projectDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).photoDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).coverPhotoDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).projectScheduleDao())
    }

    private fun getProjectScheduleRepository(context: Context): ProjectScheduleRepository {
        return ProjectScheduleRepository.getInstance(
                TimeLapseDatabase.getInstance(context.applicationContext).projectScheduleDao())
    }

    private fun getProjectTagRepository(context: Context): ProjectTagRepository {
        return ProjectTagRepository.getInstance(
                TimeLapseDatabase.getInstance(context.applicationContext).projectTagDao(),
                TimeLapseDatabase.getInstance(context.applicationContext).tagDao())
    }

    private fun getTagRepository(context: Context): TagRepository {
        return TagRepository.getInstance(
                TimeLapseDatabase.getInstance(context.applicationContext).tagDao())
    }

    fun provideCamera2ViewModelFactory(context: Context, photo: Photo?, project: Project?): Camera2ViewModelFactory {
        val projectRepository = getProjectRepository(context)
        val photoRepository = getPhotoRepository(context)
        return Camera2ViewModelFactory(
                projectRepository,
                photoRepository,
                photo,
                project)
    }

    fun provideCameraXViewModelFactory(context: Context, photo: Photo?, project: Project?): CameraXViewModelFactory {
        val projectRepository = getProjectRepository(context)
        val photoRepository = getPhotoRepository(context)
        return CameraXViewModelFactory(
                projectRepository,
                photoRepository,
                photo,
                project)
    }

    fun provideDetailsViewModelFactory(context: Context, project: Project): DetailViewModelFactory {
        val photoRepository = getPhotoRepository(context)
        val projectRepository = getProjectRepository(context)
        val projectTagRepository = getProjectTagRepository(context)
        val coverPhotoRepository = getCoverPhotoRepository(context)
        val tagRepository = getTagRepository(context)
        val projectScheduleRepository = getProjectScheduleRepository(context)
        return DetailViewModelFactory(
                photoRepository,
                projectRepository,
                projectTagRepository,
                coverPhotoRepository,
                tagRepository,
                projectScheduleRepository,
                project.project_id)
    }

    fun provideGalleryViewModelFactory(context: Context): GalleryViewModelFactory {
        val projectRepository = getProjectRepository(context)
        val tagRepository = getTagRepository(context)
        val projectTagRepository = getProjectTagRepository(context)
        return GalleryViewModelFactory(
                projectRepository,
                tagRepository,
                projectTagRepository)
    }

    fun provideSettingsViewModelFactory(): SettingsViewModelFactory {
        return SettingsViewModelFactory()
    }

    fun provideWidgetGridRemoteViewsFactory(context: Context, intent: Intent): WidgetGridRemoteViewsFactory {
        val projectRepository = getProjectRepository(context)
        val projectScheduleRepository = getProjectScheduleRepository(context)
        val coverPhotoRepository = getCoverPhotoRepository(context)
        val photoRepository = getPhotoRepository(context)
        return WidgetGridRemoteViewsFactory(
                context, projectRepository,
                coverPhotoRepository,
                photoRepository)
    }

}