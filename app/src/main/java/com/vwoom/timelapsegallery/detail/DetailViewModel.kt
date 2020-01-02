package com.vwoom.timelapsegallery.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.utils.FileUtils
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class DetailViewModel(val repository: Repository, projectId: Long) : ViewModel() {
    val photos: LiveData<List<PhotoEntry>> = repository.getPhotos(projectId)
    val tags: LiveData<List<ProjectTagEntry>> = repository.getProjectTags(projectId)

    val currentProject: LiveData<Project> = repository.getProjectView(projectId)
    val currentPhoto: MutableLiveData<PhotoEntry?> = MutableLiveData(null)

    var isPlaying: Boolean = false

    var lastPhoto: Photo? = null

    fun setLastPhotoByEntry(externalFilesDir: File, project: Project, entry: PhotoEntry){
        val url = FileUtils.getPhotoUrl(externalFilesDir, project, entry)
        lastPhoto = Photo(entry.project_id, entry.id, entry.timestamp, url)
    }

    fun nextPhoto(){
        if (isPlaying) return
        if (photos.value == null || photos.value!!.size <= 1) return
        if (currentPhoto.value == null) return
        val index: Int = photos.value!!.indexOf(currentPhoto.value!!)
        if (index == photos.value!!.size - 1) return
        currentPhoto.value = photos.value!!.get(index+1)
    }

    fun previousPhoto(){
        if (isPlaying) return
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

    fun addTag(tagText: String, project: Project){
        viewModelScope.launch {
            repository.addTagToProject(tagText, project)
        }
    }

    fun getTags(projectTags: List<ProjectTagEntry>): List<TagEntry> = runBlocking {
        repository.getTagsFromProjectTags(projectTags)
    }

    fun deleteCurrentPhoto(externalFilesDir: File){
        viewModelScope.launch {
            repository.deletePhoto(externalFilesDir, currentPhoto.value!!)
        }
    }

    fun deleteCurrentProject(externalFilesDir: File) {
        viewModelScope.launch {
            repository.deleteProject(externalFilesDir, currentProject.value?.project_id!!)
        }
    }
}