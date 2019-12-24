package com.vwoom.timelapsegallery.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.view.Photo
import kotlinx.coroutines.launch
import java.io.File

class CameraViewModel(
        private val repository: Repository,
        val photo: Photo?
) : ViewModel() {
    //val photos: LiveData<List<PhotoEntry>> = repository.getPhotos(projectId)
    //val currentProject: LiveData<Project> = repository.getProjectView(projectId)

    var resultPhoto: Photo? = null

    fun handleFile(file: File, externalFilesDir: File){
        viewModelScope.launch {
            // If no photo when constructed then we have a new project
            if (photo == null) repository.newProject(file, externalFilesDir)
            // TODO Otherwise add photo to project
            else {
                //repository.addPhotoToProject(file, externalFilesDir, project)
            }
        }
    }

    fun insertProject(file: File){
        viewModelScope.launch {
                //repository.newProject(file)
        }
    }

    fun insertPhoto(photoEntry: PhotoEntry){
        viewModelScope.launch {
            //repository.addPhotoToProject(photoEntry)
        }
    }

}