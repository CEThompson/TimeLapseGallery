package com.vwoom.timelapsegallery.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.entry.CoverPhotoEntry
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.utils.FileUtils
import kotlinx.coroutines.launch
import java.io.File

class CameraViewModel(
        private val repository: Repository,
        private val project: Project?
) : ViewModel() {
    //val photos: LiveData<List<PhotoEntry>> = repository.getPhotos(projectId)
    //val currentProject: LiveData<Project> = repository.getProjectView(projectId)

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