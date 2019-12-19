package com.vwoom.timelapsegallery.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.vwoom.timelapsegallery.data.TimeLapseDatabase.Companion.getInstance
import com.vwoom.timelapsegallery.data.view.Project

class GalleryViewModel(application: Application?) : AndroidViewModel(application!!) {
    val projects: LiveData<List<Project>>

    companion object {
        private val TAG = GalleryViewModel::class.java.simpleName
    }

    init {
        val database = getInstance(getApplication())
        projects = database.projectDao().loadProjectViews()
    }
}