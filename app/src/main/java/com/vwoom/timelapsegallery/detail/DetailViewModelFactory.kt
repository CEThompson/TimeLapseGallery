package com.vwoom.timelapsegallery.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import com.vwoom.timelapsegallery.data.repository.*

class DetailViewModelFactory(private val photoRepository: PhotoRepository,
                             private val projectRepository: ProjectRepository,
                             private val projectTagRepository: ProjectTagRepository,
                             private val coverPhotoRepository: CoverPhotoRepository,
                             private val tagRepository: TagRepository,
                             private val projectScheduleRepository: ProjectScheduleRepository,
                             private val mProjectId: Long) : NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DetailViewModel(photoRepository,
                projectRepository,
                projectTagRepository,
                coverPhotoRepository,
                tagRepository,
                projectScheduleRepository,
                mProjectId) as T
    }
}