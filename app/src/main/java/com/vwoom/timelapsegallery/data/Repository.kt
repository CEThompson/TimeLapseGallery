package com.vwoom.timelapsegallery.data

import com.vwoom.timelapsegallery.data.dao.CoverPhotoDao
import com.vwoom.timelapsegallery.data.dao.PhotoDao
import com.vwoom.timelapsegallery.data.dao.ProjectDao
import com.vwoom.timelapsegallery.data.dao.ProjectScheduleDao
import com.vwoom.timelapsegallery.data.entry.CoverPhotoEntry
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.utils.FileUtils
import java.io.File

class Repository private constructor(
        private val projectDao: ProjectDao,
        private val photoDao: PhotoDao,
        private val coverPhotoDao: CoverPhotoDao,
        private val projectScheduleDao: ProjectScheduleDao) {

    suspend fun newProject(file: File, externalFilesDir: File){

        // Create database entries
        val timestamp = System.currentTimeMillis()

        // Create and insert the project
        val projectEntry = ProjectEntry(null, 0)

        // TODO get the external files directory from application

        FileUtils.createFinalFileFromTemp(externalFilesDir, file.absolutePath, projectEntry, timestamp)

        // Insert the project
        val project_id = projectDao.insertProject(projectEntry)

        // Create and insert the photo
        val photoEntry = PhotoEntry(project_id, timestamp)

        // Insert the photo
        val photo_id = photoDao.insertPhoto(photoEntry)

        // Create and insert the cover photo
        val coverPhotoEntry = CoverPhotoEntry(project_id, photo_id)

        val projectScheduleEntry = ProjectScheduleEntry(project_id, null, null)

        // Insert the cover photo
        coverPhotoDao.insertPhoto(coverPhotoEntry)
        // Insert the project schedule
        projectScheduleDao.insertProjectSchedule(projectScheduleEntry)
    }

    suspend fun addPhotoToProject(photo: Photo){
        val photoEntry = PhotoEntry(photo.project_id, photo.photo_timestamp)
        photoDao.insertPhoto(photoEntry)
    }

    fun getProjectViews() = projectDao.loadProjectViews()

    fun getProjectView(projectId: Long) = projectDao.loadProjectView(projectId)

    fun getPhotos(projectId: Long) = photoDao.loadAllPhotosByProjectId(projectId)


    companion object {
        @Volatile private var instance: Repository? = null

        fun getInstance(
                projectDao: ProjectDao,
                photoDao: PhotoDao,
                coverPhotoDao: CoverPhotoDao,
                projectScheduleDao: ProjectScheduleDao) =
                instance ?: synchronized(this) {
                    instance ?: Repository(
                            projectDao,
                            photoDao,
                            coverPhotoDao,
                            projectScheduleDao).also { instance = it }
                }

    }

}