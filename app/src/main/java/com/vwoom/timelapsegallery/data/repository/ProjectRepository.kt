package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.data.dao.CoverPhotoDao
import com.vwoom.timelapsegallery.data.dao.PhotoDao
import com.vwoom.timelapsegallery.data.dao.ProjectDao
import com.vwoom.timelapsegallery.data.dao.ProjectScheduleDao
import com.vwoom.timelapsegallery.data.entry.CoverPhotoEntry
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ProjectRepository private constructor(private val projectDao: ProjectDao,
                                            private val photoDao: PhotoDao,
                                            private val coverPhotoDao: CoverPhotoDao,
                                            private val projectScheduleDao: ProjectScheduleDao){

    fun getProjectViews() = projectDao.getProjectViewsLiveData()

    fun getScheduledProjectViews() = projectDao.getScheduledProjectViews()

    fun getProjectView(projectId: Long) = projectDao.getProjectViewLiveData(projectId)

    suspend fun updateProjectName(externalFilesDir: File, sourceProject: Project, name: String){
        val source = projectDao.getProjectById(sourceProject.project_id)
        val destination = ProjectEntry(source.id, name)
        val success = FileUtils.renameProject(externalFilesDir, source, destination)
        if (success){
            source.project_name = destination.project_name
            projectDao.updateProject(source)
        }
    }

    suspend fun newProject(file: File, externalFilesDir: File, scheduleInterval: Int = 0, exifOrientation: Int?){
        // Create database entries
        val timestamp = System.currentTimeMillis()

        // Create and insert the project
        val projectEntry = ProjectEntry(null)
        val projectId = projectDao.insertProject(projectEntry)
        projectEntry.id = projectId

        // Create and insert the photo
        val photoEntry = PhotoEntry(projectId, timestamp)
        val photoId = photoDao.insertPhoto(photoEntry)
        photoEntry.id = photoId

        // Create cover photo and schedule then insert
        val coverPhotoEntry = CoverPhotoEntry(projectId, photoId)
        val projectScheduleEntry = ProjectScheduleEntry(projectId, scheduleInterval)
        coverPhotoDao.insertPhoto(coverPhotoEntry)
        projectScheduleDao.insertProjectSchedule(projectScheduleEntry)

        withContext(Dispatchers.IO) {
            FileUtils.createFinalFileFromTemp(externalFilesDir, file.absolutePath, projectEntry, timestamp, exifOrientation)
        }
    }

    suspend fun deleteProject(externalFilesDir: File, projectId: Long){
        val projectEntry = projectDao.getProjectById(projectId)
        // Delete files first since there is a listener on the project
        FileUtils.deleteProject(externalFilesDir, projectEntry)
        // Now remove reference from the database
        projectDao.deleteProjectByEntry(projectEntry)
    }

    companion object {
        @Volatile private var instance: ProjectRepository? = null

        fun getInstance(projectDao: ProjectDao,
                        photoDao: PhotoDao,
                        coverPhotoDao: CoverPhotoDao,
                        projectScheduleDao: ProjectScheduleDao) =
                instance ?: synchronized(this) {
                    instance ?: ProjectRepository(
                            projectDao,
                            photoDao,
                            coverPhotoDao,
                            projectScheduleDao).also { instance = it }
                }
    }

}