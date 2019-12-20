package com.vwoom.timelapsegallery.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.view.Project

class DetailsActivityViewModel(database: TimeLapseDatabase, projectId: Long) : ViewModel() {
    val photos: LiveData<List<PhotoEntry?>?>?
    val currentProject: LiveData<Project?>?

    companion object {
        private val TAG = DetailsActivityViewModel::class.java.simpleName
    }

    init {
        photos = database.photoDao()!!.loadAllPhotosByProjectId(projectId)
        currentProject = database.projectDao()!!.loadProjectView(projectId)
    }
}