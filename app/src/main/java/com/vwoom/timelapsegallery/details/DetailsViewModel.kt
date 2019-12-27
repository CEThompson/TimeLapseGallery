package com.vwoom.timelapsegallery.details

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.utils.FileUtils
import kotlinx.coroutines.launch
import java.io.File

class DetailsViewModel(val repository: Repository, projectId: Long) : ViewModel() {
    val photos: LiveData<List<PhotoEntry>> = repository.getPhotos(projectId)
    val currentProject: LiveData<Project> = repository.getProjectView(projectId)
    val currentPhoto: MutableLiveData<PhotoEntry?> = MutableLiveData(null)

    //var isPlaying: Boolean = false
    //var photoPosition: Int = photos.value!!.size

    var lastPhoto: Photo? = null

    fun setLastPhotoByEntry(externalFilesDir: File, project: Project, entry: PhotoEntry){
        val url = FileUtils.getPhotoUrl(externalFilesDir, project, entry)
        lastPhoto = Photo(entry.project_id, entry.id, entry.timestamp, url)
    }

    fun nextPhoto(){
        // TODO set current photo to next here
        /*
        if (mPlaying) return
        // Otherwise adjust the current photo to the next
        val currentIndex: Int = mPhotos.indexOf(mCurrentPhoto)
        if (currentIndex == 0) return
        mCurrentPhoto = mPhotos.get(currentIndex - 1)
        mCurrentPlayPosition = mPhotos.indexOf(mCurrentPhoto)
        loadUi(mCurrentPhoto)
         */

        if (photos.value == null || photos.value!!.size <= 1) return
        if (currentPhoto.value == null) return

        val index: Int = photos.value!!.indexOf(currentPhoto.value!!)

        if (index == photos.value!!.size - 1) return

        currentPhoto.value = photos.value!!.get(index+1)
    }

    fun previousPhoto(){
        // TODO set previous photo here
        /*
        if (mPlaying) return
        // Otherwise adjust the current photo to the previous
        val currentIndex: Int = mPhotos.indexOf(mCurrentPhoto)
        if (currentIndex == mPhotos.size() - 1) return
        mCurrentPhoto = mPhotos.get(currentIndex + 1)
        mCurrentPlayPosition = mPhotos.indexOf(mCurrentPhoto)
        loadUi(mCurrentPhoto)
         */

        if (photos.value == null || photos.value!!.size <= 0) return
        if (currentPhoto.value == null) return

        val index: Int = photos.value!!.indexOf(currentPhoto.value!!)

        if (index == 0) return

        currentPhoto.value = photos.value!!.get(index-1)

    }



    fun setPhoto(photoEntry: PhotoEntry) {
        currentPhoto.value = photoEntry
    }

    fun setCoverPhoto(photoEntry: PhotoEntry) {
        viewModelScope.launch {
            repository.setCoverPhoto(photoEntry)
        }
    }

    fun deletePhoto(photoEntry: PhotoEntry){
        viewModelScope.launch {
            repository.deletePhoto(photoEntry)
        }
    }

    fun deleteCurrentPhoto(){
        viewModelScope.launch {
            repository.deletePhoto(currentPhoto.value!!)
        }
    }

    fun deleteProject(project: Project){
        viewModelScope.launch {
            repository.deleteProject(project.project_id)
        }
    }

    fun deleteCurrentProject() {
        viewModelScope.launch {
            repository.deleteProject(currentProject?.value?.project_id!!)
        }
    }
}