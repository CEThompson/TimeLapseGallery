package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.data.dao.CoverPhotoDao
import com.vwoom.timelapsegallery.data.dao.PhotoDao
import com.vwoom.timelapsegallery.data.dao.ProjectDao
import com.vwoom.timelapsegallery.data.entry.CoverPhotoEntry
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PhotoRepository private constructor(private val photoDao: PhotoDao,
                                          private val projectDao: ProjectDao,
                                          private val coverPhotoDao: CoverPhotoDao){

    fun getPhotos(projectId: Long) = photoDao.loadAllPhotosByProjectId(projectId)

    suspend fun addPhotoToProject(file: File,
                                  externalFilesDir: File,
                                  project: Project){
        val timestamp = System.currentTimeMillis()
        val photoEntry = PhotoEntry(project.project_id, timestamp)
        val photoId = photoDao.insertPhoto(photoEntry)

        val coverPhotoEntry = CoverPhotoEntry(project.project_id, photoId)
        coverPhotoDao.insertPhoto(coverPhotoEntry)

        val projectEntry = projectDao.loadProjectById(project.project_id)

        withContext(Dispatchers.IO) {
            FileUtils.createFinalFileFromTemp(externalFilesDir, file.absolutePath, projectEntry, timestamp)
        }
    }

    suspend fun deletePhoto(externalFilesDir: File, photoEntry: PhotoEntry){
        val projectEntry = projectDao.loadProjectById(photoEntry.project_id)
        FileUtils.deletePhoto(externalFilesDir, projectEntry, photoEntry)
        photoDao.deletePhoto(photoEntry)
    }

    companion object {
        @Volatile private var instance: PhotoRepository? = null

        fun getInstance(photoDao: PhotoDao, projectDao: ProjectDao, coverPhotoDao: CoverPhotoDao) =
                instance ?: synchronized(this) {
                    instance ?: PhotoRepository(photoDao, projectDao, coverPhotoDao).also { instance = it }
                }
    }

}