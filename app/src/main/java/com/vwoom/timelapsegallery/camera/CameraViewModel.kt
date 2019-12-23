package com.vwoom.timelapsegallery.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.view.Project
import kotlinx.coroutines.launch
import java.io.File

class CameraViewModel(
        private val repository: Repository,
        private val project: Project?
) : ViewModel() {
    //val photos: LiveData<List<PhotoEntry>> = repository.getPhotos(projectId)
    //val currentProject: LiveData<Project> = repository.getProjectView(projectId)

    var lastPhoto: PhotoEntry? = null

    init {
        if (project != null) lastPhoto = repository.getLastPhotoInProject(project.project_id)
    }

    fun handleFile(file: File, externalFilesDir: File){
        viewModelScope.launch {
            if (project == null) repository.newProject(file, externalFilesDir)
            //else repository.addPhotoToProject(file, externalFilesDir, project)
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