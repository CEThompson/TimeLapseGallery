package com.vwoom.timelapsegallery.utils

import android.content.Context
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.repository.ProjectRepository

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