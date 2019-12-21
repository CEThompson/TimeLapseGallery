package com.vwoom.timelapsegallery.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.view.Project

class GalleryViewModel internal constructor(repository: Repository) : ViewModel() {
    val projects: LiveData<List<Project>> = repository.getProjectViews()
}