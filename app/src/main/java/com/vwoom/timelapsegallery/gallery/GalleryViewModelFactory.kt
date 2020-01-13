package com.vwoom.timelapsegallery.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.repository.ProjectTagRepository
import com.vwoom.timelapsegallery.data.repository.TagRepository
import com.vwoom.timelapsegallery.gallery.GalleryViewModel

class GalleryViewModelFactory(private val projectRepository: ProjectRepository,
                              private val tagRepository: TagRepository,
                              private val projectTagRepository: ProjectTagRepository) : NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return GalleryViewModel(
                projectRepository,
                tagRepository,
                projectTagRepository) as T
    }
}