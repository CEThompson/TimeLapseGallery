package com.vwoom.timelapsegallery.di

import android.content.Context
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.repository.ProjectRepository

// NOTE: this util is retained due to simplicity for injecting into work manager
// (dagger injection looks complex in comparison)
// Used for injecting into services, broadcast receivers, and workers
object InjectorUtils {
    fun getProjectRepository(context: Context): ProjectRepository {
        return ProjectRepository.getInstance(
                TimeLapseDatabase.getInstance(context).projectDao(),
                TimeLapseDatabase.getInstance(context).photoDao(),
                TimeLapseDatabase.getInstance(context).coverPhotoDao(),
                TimeLapseDatabase.getInstance(context).projectScheduleDao()
        )
    }
}