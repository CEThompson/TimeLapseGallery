package com.vwoom.timelapsegallery.data

import android.util.Log
import com.vwoom.timelapsegallery.data.dao.CoverPhotoDao
import com.vwoom.timelapsegallery.data.dao.PhotoDao
import com.vwoom.timelapsegallery.data.dao.ProjectDao
import com.vwoom.timelapsegallery.data.dao.ProjectScheduleDao
import com.vwoom.timelapsegallery.data.entry.CoverPhotoEntry
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project
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
        val project_id = projectDao.insertProject(projectEntry)
        projectEntry.id = project_id

        // Create and insert the photo
        val photoEntry = PhotoEntry(project_id, timestamp)
        val photo_id = photoDao.insertPhoto(photoEntry)
        photoEntry.id = photo_id


        // Create cover photo and schedule then insert
        val coverPhotoEntry = CoverPhotoEntry(project_id, photo_id)
        val projectScheduleEntry = ProjectScheduleEntry(project_id, null, null)
        coverPhotoDao.insertPhoto(coverPhotoEntry)
        projectScheduleDao.insertProjectSchedule(projectScheduleEntry)
        
        Log.d("TimeLapseRepository", "$projectEntry")
        Log.d("TimeLapseRepository", "$photoEntry")
        Log.d("TimeLapseRepository", "$coverPhotoEntry")
        Log.d("TimeLapseRepository", "$projectScheduleEntry")

        // TODO set up a work manager to handle file operations
        FileUtils.createFinalFileFromTemp(externalFilesDir, file.absolutePath, projectEntry, timestamp)
    }

    suspend fun addPhotoToProject(file: File, externalFilesDir: File, project: Project){
        val timestamp = System.currentTimeMillis()
        val photoEntry = PhotoEntry(project.project_id, timestamp)
        val photoId = photoDao.insertPhoto(photoEntry)

        val coverPhotoEntry = CoverPhotoEntry(project.project_id, photoId)
        coverPhotoDao.insertPhoto(coverPhotoEntry)

        // TODO set up a work manager to handle file operations
        FileUtils.createFinalFileFromTemp(externalFilesDir, file.absolutePath, project, timestamp)
    }

    fun getProjectViews() = projectDao.loadProjectViews()

    fun getProjectView(projectId: Long) = projectDao.loadProjectView(projectId)

    fun getPhotos(projectId: Long) = photoDao.loadAllPhotosByProjectId(projectId)

    fun getLastPhotoInProject(projectId: Long) = photoDao.getLastPhoto(projectId)

    fun getPhoto(projectId: Long, photoId: Long) = photoDao.loadPhoto(projectId, photoId)

    suspend fun setCoverPhoto(photoEntry: PhotoEntry) {
        coverPhotoDao.insertPhoto(CoverPhotoEntry(photoEntry.project_id, photoEntry.id))
    }


    suspend fun deletePhoto(photoEntry: PhotoEntry){ photoDao.deletePhoto(photoEntry) }
    suspend fun deleteProject(projectId: Long){ projectDao.deleteProject(projectId) }

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