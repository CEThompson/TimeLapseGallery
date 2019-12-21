package com.vwoom.timelapsegallery.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.view.Project

class GalleryViewModel internal constructor(repository: Repository) : ViewModel() {
    val projects: LiveData<List<Project>> = repository.getProjectViews()

    companion object {
        private val TAG = GalleryViewModel::class.java.simpleName
    }
}