package com.vwoom.timelapsegallery.camera2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project

class Camera2ViewModelFactory(private val projectRepository: ProjectRepository,
                              private val photo: Photo?,
                              private val project: Project?) : NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return Camera2ViewModel(
                projectRepository,
                photo,
                project) as T
    }
}