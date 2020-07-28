package com.vwoom.timelapsegallery.camera2

import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.ProjectView
import java.io.File
import javax.inject.Inject

class Camera2ViewModel @Inject constructor(
        private val projectRepository: ProjectRepository) : ViewModel() {

    var photo: Photo? = null
    var projectView: ProjectView? = null

    fun setProjectInfo(projectView: ProjectView?, photo: Photo?){
        this.photo = photo
        this.projectView = projectView
    }

    // Returns a project view for navigation
    suspend fun addNewProject(file: File, externalFilesDir: File, timestamp: Long): ProjectView {
        return projectRepository.newProject(file, externalFilesDir, timestamp, 0)
    }

    // Adds the photo to the current project
    suspend fun addPhotoToProject(file: File, externalFilesDir: File, timestamp: Long) {
        projectView?.let {
            projectRepository.addPhotoToProject(file, externalFilesDir, it, timestamp)
        }
    }

}