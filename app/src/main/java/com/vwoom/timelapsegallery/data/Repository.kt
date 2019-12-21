package com.vwoom.timelapsegallery.data

import android.content.Context
import com.vwoom.timelapsegallery.data.dao.PhotoDao
import com.vwoom.timelapsegallery.data.dao.ProjectDao
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry

class Repository private constructor(private val projectDao: ProjectDao, private val photoDao: PhotoDao) {

    suspend fun createProject(projectEntry: ProjectEntry){
        projectDao.insertProject(projectEntry)
    }

    suspend fun addPhotoToProject(photoEntry: PhotoEntry){
        photoDao.insertPhoto(photoEntry)
    }

    fun getProjectViews() = projectDao.loadProjectViews()

    fun getProjectView(projectId: Long) = projectDao.loadProjectView(projectId)

    fun getPhotos(projectId: Long) = photoDao.loadAllPhotosByProjectId(projectId)


    companion object {
        @Volatile private var instance: Repository? = null

        fun getInstance(projectDao: ProjectDao, photoDao: PhotoDao) =
                instance ?: synchronized(this) {
                    instance ?: Repository(projectDao, photoDao).also { instance = it }
                }

    }

}