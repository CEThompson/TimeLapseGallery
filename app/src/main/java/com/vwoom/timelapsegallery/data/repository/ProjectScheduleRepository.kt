package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.data.dao.ProjectScheduleDao

class ProjectScheduleRepository private constructor(private val projectScheduleDao: ProjectScheduleDao){

    companion object {
        @Volatile private var instance: ProjectScheduleRepository? = null

        fun getInstance(projectScheduleDao: ProjectScheduleDao) =
                instance ?: synchronized(this) {
                    instance ?: ProjectScheduleRepository(projectScheduleDao).also { instance = it }
                }
    }

    fun getProjectSchedule(projectId: Long) = projectScheduleDao.getProjectScheduleByProjectId(projectId)
}