package com.vwoom.timelapsegallery.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import com.vwoom.timelapsegallery.data.Repository

class DetailViewModelFactory(private val repository: Repository, private val mProjectId: Long) : NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return DetailViewModel(repository, mProjectId) as T
    }
}