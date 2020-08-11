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
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class ProjectRepository
@Inject constructor(private val projectDao: ProjectDao,
                    private val photoDao: PhotoDao,
                    private val coverPhotoDao: CoverPhotoDao,
                    private val projectScheduleDao: ProjectScheduleDao) : IProjectRepository {

    var coroutineContext: CoroutineContext = Dispatchers.IO

    /**
     * Project observables
     */
    override fun getProjectViewLiveData(projectId: Long) = projectDao.getProjectViewLiveData(projectId)
    override fun getProjectViewsLiveData() = projectDao.getProjectViewsLiveData()
    override fun getScheduledProjectViews() = projectDao.getScheduledProjectViews()
    fun getAllProjects() = projectDao.getProjects()

    /**
     * Project updating and deletion
     */
    override suspend fun newProject(file: File,
                                    externalFilesDir: File,
                                    timestamp: Long,
                                    scheduleInterval: Int): ProjectView {
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
        val coverPhotoId = coverPhotoDao.insertPhoto(coverPhotoEntry)
        projectScheduleDao.insertProjectSchedule(projectScheduleEntry)

        // TODO: handle blocking method in non blocking context
        @Suppress("BlockingMethodInNonBlockingContext")
        withContext(coroutineContext) {
            FileUtils.createFinalFileFromTemp(externalFilesDir, file.absolutePath, projectEntry, timestamp)
        }

        return ProjectView(projectEntry.id, projectEntry.project_name, scheduleInterval, coverPhotoId, timestamp)
    }

    override suspend fun updateProjectName(externalFilesDir: File, sourceProjectView: ProjectView, name: String) {
        val source: ProjectEntry = projectDao.getProjectById(sourceProjectView.project_id) ?: return
        val destination = ProjectEntry(source.id, name)
        withContext(coroutineContext) {
            val success = ProjectUtils.renameProject(externalFilesDir, source, destination)
            if (success) {
                source.project_name = destination.project_name
                projectDao.updateProject(source)
            }
        }
    }

    override suspend fun deleteProject(externalFilesDir: File, projectId: Long) {
        val projectEntry = projectDao.getProjectById(projectId) ?: return
        withContext(coroutineContext) {
            // Delete files first since there is a listener on the project
            ProjectUtils.deleteProject(externalFilesDir, projectEntry)
            // Now remove reference from the database
            projectDao.deleteProjectByEntry(projectEntry)
        }
    }

    /**
     * Project scheduling and cover photo
     */
    override suspend fun setProjectSchedule(
            externalFilesDir: File,
            projectView: ProjectView,
            projectScheduleEntry: ProjectScheduleEntry) {

        // Write the project schedule to the database
        projectScheduleDao.insertProjectSchedule(projectScheduleEntry)

        withContext(coroutineContext) {
            // Handle the file representation of the schedule
            FileUtils.writeProjectScheduleFile(externalFilesDir, projectView.project_id, projectScheduleEntry)
        }
    }

    override suspend fun setProjectCoverPhoto(entry: PhotoEntry) {
        coverPhotoDao.insertPhoto(CoverPhotoEntry(entry.project_id, entry.id))
    }

    /**
     * Project photo management and observables
     */

    override fun getProjectPhotosLiveData(projectId: Long) = photoDao.getPhotosLiveDataByProjectId(projectId)

    override suspend fun addPhotoToProject(file: File,
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
        // TODO: handle blocking method in non blocking context
        @Suppress("BlockingMethodInNonBlockingContext")
        withContext(coroutineContext) {
            FileUtils.createFinalFileFromTemp(externalFilesDir, file.absolutePath, projectEntry, timestamp)
        }
    }

    override suspend fun deleteProjectPhoto(externalFilesDir: File, photoEntry: PhotoEntry) {
        val projectEntry = projectDao.getProjectById(photoEntry.project_id) ?: return
        withContext(coroutineContext) {
            ProjectUtils.deleteProjectPhoto(externalFilesDir, projectEntry, photoEntry)
        }
        photoDao.deletePhoto(photoEntry)
    }

    // Note: instance is used by injector utils for the repository, it is not used by dagger 2
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