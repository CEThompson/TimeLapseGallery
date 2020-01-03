package com.vwoom.timelapsegallery.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.gallery.GalleryViewModel

class GalleryViewModelFactory(private val repository: Repository) : NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return GalleryViewModel(repository) as T
    }
}