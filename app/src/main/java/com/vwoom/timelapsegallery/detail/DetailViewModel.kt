package com.vwoom.timelapsegallery.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.repository.*
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.utils.FileUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class DetailViewModel(val photoRepository: PhotoRepository,
                      val projectRepository: ProjectRepository,
                      val projectTagRepository: ProjectTagRepository,
                      val coverPhotoRepository: CoverPhotoRepository,
                      val tagRepository: TagRepository,
                      projectId: Long) : ViewModel() {
    val photos: LiveData<List<PhotoEntry>> = photoRepository.getPhotos(projectId)
    val tags: LiveData<List<ProjectTagEntry>> = projectTagRepository.getProjectTags(projectId)

    val currentProject: LiveData<Project> = projectRepository.getProjectView(projectId)
    val currentPhoto: MutableLiveData<PhotoEntry?> = MutableLiveData(null)

    var isPlaying: Boolean = false

    var lastPhoto: Photo? = null

    fun deleteTags(tags: ArrayList<String>, project: Project){
        viewModelScope.launch {
            projectTagRepository.deleteTags(tags, project)
        }
    }

    fun updateProjectName(externalFilesDir: File, name: String, source: Project){
        viewModelScope.launch {
            projectRepository.updateProjectName(externalFilesDir, source, name)
        }
    }

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
            coverPhotoRepository.setCoverPhoto(photoEntry)
        }
    }

    fun addTag(tagText: String, project: Project){
        viewModelScope.launch {
            projectTagRepository.addTagToProject(tagText, project)
        }
    }

    fun getTags(projectTags: List<ProjectTagEntry>): List<TagEntry> = runBlocking {
        tagRepository.getTagsFromProjectTags(projectTags)
    }

    fun deleteCurrentPhoto(externalFilesDir: File){
        viewModelScope.launch {
            photoRepository.deletePhoto(externalFilesDir, currentPhoto.value!!)
        }
    }

    fun deleteCurrentProject(externalFilesDir: File) {
        viewModelScope.launch {
            projectRepository.deleteProject(externalFilesDir, currentProject.value?.project_id!!)
        }
    }
}