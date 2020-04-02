package com.vwoom.timelapsegallery.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.repository.TagRepository

class DetailViewModelFactory(private val projectRepository: ProjectRepository,
                             private val tagRepository: TagRepository,
                             private val mProjectId: Long) : NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DetailViewModel(
                projectRepository,
                tagRepository,
                mProjectId) as T
    }
}