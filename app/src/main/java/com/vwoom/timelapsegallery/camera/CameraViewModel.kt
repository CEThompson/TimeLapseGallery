package com.vwoom.timelapsegallery.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vwoom.timelapsegallery.data.Repository
import kotlinx.coroutines.launch

class CameraViewModel(
        private val repository: Repository,
        private val projectId: Long?
) : ViewModel() {
    //val photos: LiveData<List<PhotoEntry>> = repository.getPhotos(projectId)
    //val currentProject: LiveData<Project> = repository.getProjectView(projectId)

    fun addProject(){
        viewModelScope.launch {
                //repository.createProject()
        }
    }

}