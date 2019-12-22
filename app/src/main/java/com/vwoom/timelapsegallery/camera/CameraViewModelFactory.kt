package com.vwoom.timelapsegallery.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.view.Project

class CameraViewModelFactory(private val repository: Repository, private val project: Project?) : NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return CameraViewModel(repository, project) as T
    }
}