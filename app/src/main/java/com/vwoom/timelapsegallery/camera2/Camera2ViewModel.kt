package com.vwoom.timelapsegallery.camera2

import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.ProjectView
import java.io.File

class Camera2ViewModel(
        private val projectRepository: ProjectRepository,
        val photo: Photo?,
        val projectView: ProjectView?
) : ViewModel() {
    // Returns a project view for navigation
    suspend fun addNewProject(file: File, externalFilesDir: File, timestamp: Long): ProjectView {
        return projectRepository.newProject(file, externalFilesDir, timestamp, 0)
    }

    // Adds the photo to the current project
    suspend fun addPhotoToProject(file: File, externalFilesDir: File, timestamp: Long) {
        projectView ?: return
        projectRepository.addPhotoToProject(file, externalFilesDir, projectView, timestamp)
    }

}