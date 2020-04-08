package com.vwoom.timelapsegallery.cameraX

import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.ProjectView
import java.io.File

class CameraXViewModel(
        private val projectRepository: ProjectRepository,
        val photo: Photo?,
        val projectView: ProjectView?
) : ViewModel() {
    suspend fun handleFile(file: File, externalFilesDir: File) {
        // If no photo when constructed then we have a new project
        if (photo == null) projectRepository.newProject(file, externalFilesDir, System.currentTimeMillis(), 0)
        // Otherwise add photo to project
        else {
            projectRepository.addPhotoToProject(file, externalFilesDir, projectView!!, System.currentTimeMillis())
        }
    }
}