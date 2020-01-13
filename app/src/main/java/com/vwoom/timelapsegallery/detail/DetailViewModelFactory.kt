package com.vwoom.timelapsegallery.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import com.vwoom.timelapsegallery.data.repository.*

class DetailViewModelFactory(private val photoRepository: PhotoRepository,
                             private val projectRepository: ProjectRepository,
                             private val projectTagRepository: ProjectTagRepository,
                             private val coverPhotoRepository: CoverPhotoRepository,
                             private val tagRepository: TagRepository,
                             private val mProjectId: Long) : NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return DetailViewModel(photoRepository,
                projectRepository,
                projectTagRepository,
                coverPhotoRepository,
                tagRepository,
                mProjectId) as T
    }
}