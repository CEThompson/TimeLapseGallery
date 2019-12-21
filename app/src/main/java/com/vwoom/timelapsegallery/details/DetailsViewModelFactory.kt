package com.vwoom.timelapsegallery.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import com.vwoom.timelapsegallery.data.Repository

class DetailsViewModelFactory(private val repository: Repository, private val mProjectId: Long) : NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return DetailsViewModel(repository, mProjectId) as T
    }
}