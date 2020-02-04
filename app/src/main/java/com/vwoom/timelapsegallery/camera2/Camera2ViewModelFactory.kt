package com.vwoom.timelapsegallery.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import com.vwoom.timelapsegallery.data.repository.PhotoRepository
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project

class Camera2ViewModelFactory(private val projectRepository: ProjectRepository,
                              private val photoRepository: PhotoRepository,
                              private val photo: Photo?,
                              private val project: Project?) : NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return Camera2ViewModel(
                projectRepository,
                photoRepository,
                photo,
                project) as T
    }
}