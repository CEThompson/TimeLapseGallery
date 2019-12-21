package com.vwoom.timelapsegallery.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.view.Project

class DetailsActivityViewModel(repository: Repository, projectId: Long) : ViewModel() {
    val photos: LiveData<List<PhotoEntry>> = repository.getPhotos(projectId)
    val currentProject: LiveData<Project> = repository.getProjectView(projectId)

    companion object {
        private val TAG = DetailsActivityViewModel::class.java.simpleName
    }
}