package com.vwoom.timelapsegallery.cameraX

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.ProjectView

class CameraXViewModelFactory(private val projectRepository: ProjectRepository,
                              private val photo: Photo?,
                              private val projectView: ProjectView?) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CameraXViewModel(
                projectRepository,
                photo,
                projectView) as T
    }
}