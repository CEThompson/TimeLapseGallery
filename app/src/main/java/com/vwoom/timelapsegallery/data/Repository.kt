package com.vwoom.timelapsegallery.data

import com.vwoom.timelapsegallery.data.dao.PhotoDao
import com.vwoom.timelapsegallery.data.dao.ProjectDao
import com.vwoom.timelapsegallery.data.entry.ProjectEntry

class Repository private constructor(private val projectDao: ProjectDao, private val photoDao: PhotoDao) {

    suspend fun createProject(projectEntry: ProjectEntry){
        projectDao.insertProject(projectEntry)
    }

    fun getProjectViews() = projectDao.loadProjectViews()

    fun getProjectView(projectId: Long) = projectDao.loadProjectView(projectId)

    fun getPhotos(projectId: Long) = photoDao.loadAllPhotosByProjectId(projectId)
}