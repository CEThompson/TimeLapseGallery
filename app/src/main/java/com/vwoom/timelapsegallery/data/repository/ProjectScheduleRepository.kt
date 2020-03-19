package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.data.dao.ProjectScheduleDao
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.utils.FileUtils
import java.io.File

class ProjectScheduleRepository private constructor(private val projectScheduleDao: ProjectScheduleDao) {

    companion object {
        @Volatile private var instance: ProjectScheduleRepository? = null

        fun getInstance(projectScheduleDao: ProjectScheduleDao) =
                instance ?: synchronized(this) {
                    instance ?: ProjectScheduleRepository(projectScheduleDao).also { instance = it }
                }
    }

    suspend fun setProjectSchedule(externalFilesDir: File, project: Project, projectScheduleEntry: ProjectScheduleEntry) {
        // Write the project schedule to the database
        projectScheduleDao.insertProjectSchedule(projectScheduleEntry)

        // Handle the file representation of the schedule
        FileUtils.scheduleProject(externalFilesDir, project, projectScheduleEntry)
    }
}