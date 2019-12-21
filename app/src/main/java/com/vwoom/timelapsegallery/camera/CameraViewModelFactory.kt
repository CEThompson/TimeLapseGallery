package com.vwoom.timelapsegallery.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.view.Photo

class CameraViewModelFactory(private val repository: Repository, private val photo: Photo?) : NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return CameraViewModel(repository, photo) as T
    }
}