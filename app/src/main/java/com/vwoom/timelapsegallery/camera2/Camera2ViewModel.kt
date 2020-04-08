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
    suspend fun handleFinalPhotoFile(file: File, externalFilesDir: File, timestamp: Long) {
        // If no project passed in when constructed then create a new project
        if (projectView == null) projectRepository.newProject(file, externalFilesDir, timestamp,0)
        // Otherwise add photo to the project
        else {
            projectRepository.addPhotoToProject(file, externalFilesDir, projectView, timestamp)
        }
    }
}