package com.vwoom.timelapsegallery.cameraX

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vwoom.timelapsegallery.camera2.Camera2ViewModel
import com.vwoom.timelapsegallery.data.repository.PhotoRepository
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project

class CameraXViewModelFactory(private val projectRepository: ProjectRepository,
                              private val photoRepository: PhotoRepository,
                              private val photo: Photo?,
                              private val project: Project?) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CameraXViewModel(
                projectRepository,
                photoRepository,
                photo,
                project) as T
    }
}