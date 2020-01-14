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
import java.io.File

class ProjectRepository private constructor(val projectDao: ProjectDao,
                                            val photoDao: PhotoDao,
                                            val coverPhotoDao: CoverPhotoDao,
                                            val projectScheduleDao: ProjectScheduleDao){

    fun getProjectViews() = projectDao.loadProjectViews()

    fun getProjectView(projectId: Long) = projectDao.loadProjectView(projectId)

    suspend fun updateProjectName(externalFilesDir: File, sourceProject: Project, name: String){
        val source = projectDao.loadProjectById(sourceProject.project_id)
        val destination = ProjectEntry(source.id, name, sourceProject.cover_set_by_user)
        val success = FileUtils.renameProject(externalFilesDir, source, destination)
        if (success){
            source.project_name = destination.project_name
            projectDao.updateProject(source)
        }
    }

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

        // TODO set up a work manager to handle file operations
        FileUtils.createFinalFileFromTemp(externalFilesDir, file.absolutePath, projectEntry, timestamp)
    }

    suspend fun deleteProject(externalFilesDir: File, projectId: Long){
        val projectEntry = projectDao.loadProjectById(projectId)
        // Delete files first since there is a listener on the project
        FileUtils.deleteProject(externalFilesDir, projectEntry)
        // Now remove reference from the database
        projectDao.deleteProject(projectEntry)
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