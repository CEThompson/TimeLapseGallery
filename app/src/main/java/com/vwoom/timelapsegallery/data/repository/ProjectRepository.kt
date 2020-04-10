package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.data.dao.CoverPhotoDao
import com.vwoom.timelapsegallery.data.dao.PhotoDao
import com.vwoom.timelapsegallery.data.dao.ProjectDao
import com.vwoom.timelapsegallery.data.dao.ProjectScheduleDao
import com.vwoom.timelapsegallery.data.entry.CoverPhotoEntry
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ProjectRepository private constructor(private val projectDao: ProjectDao,
                                            private val photoDao: PhotoDao,
                                            private val coverPhotoDao: CoverPhotoDao,
                                            private val projectScheduleDao: ProjectScheduleDao) {
    /**
     * Project observables
     */
    fun getScheduledProjectViews() = projectDao.getScheduledProjectViews()
    fun getProjectViewLiveData(projectId: Long) = projectDao.getProjectViewLiveData(projectId)
    fun getProjectViewsLiveData() = projectDao.getProjectViewsLiveData()

    /**
     * Project updating and deletion
     */
    suspend fun newProject(file: File, externalFilesDir: File, timestamp: Long, scheduleInterval: Int = 0) {
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
            FileUtils.createFinalFileFromTemp(externalFilesDir, file.absolutePath, projectEntry, timestamp)
        }
    }

    suspend fun updateProjectName(externalFilesDir: File, sourceProjectView: ProjectView, name: String) {
        val source: ProjectEntry = projectDao.getProjectById(sourceProjectView.project_id) ?: return
        val destination = ProjectEntry(source.id, name)
        withContext(Dispatchers.IO) {
            val success = ProjectUtils.renameProject(externalFilesDir, source, destination)
            if (success) {
                source.project_name = destination.project_name
                projectDao.updateProject(source)
            }
        }
    }

    suspend fun deleteProject(externalFilesDir: File, projectId: Long) {
        val projectEntry = projectDao.getProjectById(projectId) ?: return
        withContext(Dispatchers.IO) {
            // Delete files first since there is a listener on the project
            ProjectUtils.deleteProject(externalFilesDir, projectEntry)
            // Now remove reference from the database
            projectDao.deleteProjectByEntry(projectEntry)
        }
    }

    /**
     * Project scheduling and cover photo
     */
    suspend fun setProjectSchedule(
            externalFilesDir: File,
            projectView: ProjectView,
            projectScheduleEntry: ProjectScheduleEntry) {

        // Write the project schedule to the database
        projectScheduleDao.insertProjectSchedule(projectScheduleEntry)

        withContext(Dispatchers.IO) {
            // Handle the file representation of the schedule
            FileUtils.writeProjectScheduleFile(externalFilesDir, projectView.project_id, projectScheduleEntry)
        }
    }

    suspend fun setProjectCoverPhoto(entry: PhotoEntry) {
        coverPhotoDao.insertPhoto(CoverPhotoEntry(entry.project_id, entry.id))
    }

    /**
     * Project photo management and observables
     */

    fun getProjectPhotosLiveData(projectId: Long) = photoDao.getPhotosLiveDataByProjectId(projectId)

    suspend fun addPhotoToProject(file: File,
                                  externalFilesDir: File,
                                  projectView: ProjectView, timestamp: Long) {
        // Do not add photo if project cannot be found
        val projectEntry = projectDao.getProjectById(projectView.project_id) ?: return

        // Insert the photo
        val photoEntry = PhotoEntry(projectView.project_id, timestamp)
        val photoId = photoDao.insertPhoto(photoEntry)

        // Insert the cover photo
        val coverPhotoEntry = CoverPhotoEntry(projectView.project_id, photoId)
        coverPhotoDao.insertPhoto(coverPhotoEntry)

        // Create the final file
        withContext(Dispatchers.IO) {
            FileUtils.createFinalFileFromTemp(externalFilesDir, file.absolutePath, projectEntry, timestamp)
        }
    }

    suspend fun deleteProjectPhoto(externalFilesDir: File, photoEntry: PhotoEntry) {
        val projectEntry = projectDao.getProjectById(photoEntry.project_id) ?: return
        withContext(Dispatchers.IO) {
            ProjectUtils.deleteProjectPhoto(externalFilesDir, projectEntry, photoEntry)
        }
        photoDao.deletePhoto(photoEntry)
    }

    companion object {
        @Volatile
        private var instance: ProjectRepository? = null

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