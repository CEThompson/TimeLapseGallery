package com.vwoom.timelapsegallery.camera2

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
    suspend fun handleFinalPhotoFile(file: File, externalFilesDir: File){
        // If no project passed in when constructed then create a new project
        if (project == null) projectRepository.newProject(file, externalFilesDir,0)
        // Otherwise add photo to project
        else {
            photoRepository.addPhotoToProject(file, externalFilesDir, project)
        }
    }
}