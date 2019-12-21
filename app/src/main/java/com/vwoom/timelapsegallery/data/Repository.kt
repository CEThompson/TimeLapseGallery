package com.vwoom.timelapsegallery.data

import com.vwoom.timelapsegallery.data.dao.ProjectDao
import com.vwoom.timelapsegallery.data.entry.ProjectEntry

class Repository private constructor(private val projectDao: ProjectDao) {

    suspend fun createProject(projectEntry: ProjectEntry){
        projectDao.insertProject(projectEntry)
    }

    fun getProjectViews() = projectDao.loadProjectViews()
}