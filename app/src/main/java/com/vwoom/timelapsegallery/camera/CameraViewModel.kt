package com.vwoom.timelapsegallery.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project
import kotlinx.coroutines.launch
import java.io.File

class CameraViewModel(
        private val repository: Repository,
        val photo: Photo?,
        val project: Project?
) : ViewModel() {

    var resultPhoto: Photo? = null

    fun handleFile(file: File, externalFilesDir: File){
        viewModelScope.launch {
            // If no photo when constructed then we have a new project
            if (photo == null) repository.newProject(file, externalFilesDir)
            // Otherwise add photo to project
            else {
                repository.addPhotoToProject(file, externalFilesDir, project!!)
            }
        }
    }
}