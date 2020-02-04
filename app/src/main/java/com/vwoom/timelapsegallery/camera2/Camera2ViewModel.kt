package com.vwoom.timelapsegallery.detail

import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.repository.PhotoRepository
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project
import java.io.File

class Camera2ViewModel(
        private val projectRepository: ProjectRepository,
        private val photoRepository: PhotoRepository,
        val photo: Photo?,
        val project: Project?
) : ViewModel() {
    suspend fun handleFile(file: File, externalFilesDir: File){
        // If no photo when constructed then we have a new project
        if (photo == null) projectRepository.newProject(file, externalFilesDir)
        // Otherwise add photo to project
        else {
            photoRepository.addPhotoToProject(file, externalFilesDir, project!!)
        }
    }
}