package com.vwoom.timelapsegallery.data.repository

import android.util.Log
import com.vwoom.timelapsegallery.data.dao.ProjectScheduleDao
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry

class ProjectScheduleRepository private constructor(private val projectScheduleDao: ProjectScheduleDao){

    companion object {
        @Volatile private var instance: ProjectScheduleRepository? = null

        fun getInstance(projectScheduleDao: ProjectScheduleDao) =
                instance ?: synchronized(this) {
                    instance ?: ProjectScheduleRepository(projectScheduleDao).also { instance = it }
                }
    }

    suspend fun getProjectSchedule(projectId: Long): ProjectScheduleEntry? = projectScheduleDao.getProjectScheduleByProjectId(projectId)

    fun getProjectScheduleNonSuspend(projectId: Long): ProjectScheduleEntry? = projectScheduleDao.getProjectScheduleByProjectIdNonSuspend(projectId)

    suspend fun setProjectSchedule(projectScheduleEntry: ProjectScheduleEntry){
        // Toggle the project schedule
        if (projectScheduleEntry.interval_days == null || projectScheduleEntry.interval_days == 0){
            projectScheduleEntry.interval_days = 1
        } else {
            projectScheduleEntry.interval_days = 0
        }
        projectScheduleDao.insertProjectSchedule(projectScheduleEntry)
    }
}