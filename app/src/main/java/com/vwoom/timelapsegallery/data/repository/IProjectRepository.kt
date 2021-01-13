package com.vwoom.timelapsegallery.data.repository

import androidx.lifecycle.LiveData
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.view.ProjectView
import java.io.File

interface IProjectRepository {
    // Project observables
    fun getProjectViewLiveData(projectId: Long): LiveData<ProjectView?>
    fun getProjectViewsLiveData(): LiveData<List<ProjectView>>
    fun getScheduledProjectViews(): List<ProjectView>
    fun getProjectPhotosLiveData(projectId: Long): LiveData<List<PhotoEntry>>

    // For project creation and management
    suspend fun newProject(file: File, externalFilesDir: File, timestamp: Long, scheduleInterval: Int = 0): ProjectView
    suspend fun updateProjectName(externalFilesDir: File, sourceProjectView: ProjectView, name: String)
    suspend fun deleteProject(externalFilesDir: File, projectId: Long)
    suspend fun markProjectChanged(projectEntry: ProjectEntry)
    suspend fun markProjectUnchanged(projectEntry: ProjectEntry)

    // For managing project cover photo and schedules
    suspend fun setProjectSchedule(
            externalFilesDir: File,
            projectView: ProjectView,
            projectScheduleEntry: ProjectScheduleEntry)
    suspend fun setProjectCoverPhoto(entry: PhotoEntry)

    // For managing project photos
    suspend fun addPhotoToProject(file: File,
                                  externalFilesDir: File,
                                  projectView: ProjectView, timestamp: Long)
    suspend fun deleteProjectPhoto(externalFilesDir: File, photoEntry: PhotoEntry)
}