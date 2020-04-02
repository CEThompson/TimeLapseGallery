package com.vwoom.timelapsegallery.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.repository.TagRepository

class GalleryViewModelFactory(private val projectRepository: ProjectRepository,
                              private val tagRepository: TagRepository) : NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return GalleryViewModel(
                projectRepository,
                tagRepository) as T
    }
}