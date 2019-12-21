package com.vwoom.timelapsegallery.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.view.Photo
import kotlinx.coroutines.launch

class CameraViewModel(
        private val repository: Repository,
        private val photo: Photo?
) : ViewModel() {
    //val photos: LiveData<List<PhotoEntry>> = repository.getPhotos(projectId)
    //val currentProject: LiveData<Project> = repository.getProjectView(projectId)

    fun insertProject(){
        viewModelScope.launch {
                //repository.createProject()
        }
    }

}