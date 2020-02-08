package com.vwoom.timelapsegallery.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vwoom.timelapsegallery.data.entry.*
import com.vwoom.timelapsegallery.data.repository.*
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.utils.FileUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class DetailViewModel(private val photoRepository: PhotoRepository,
                      private val projectRepository: ProjectRepository,
                      private val projectTagRepository: ProjectTagRepository,
                      private val coverPhotoRepository: CoverPhotoRepository,
                      private val tagRepository: TagRepository,
                      private val projectScheduleRepository: ProjectScheduleRepository,
                      projectId: Long) : ViewModel() {
    val photos: LiveData<List<PhotoEntry>> = photoRepository.getPhotos(projectId)
    val projectTags: LiveData<List<ProjectTagEntry>> = projectTagRepository.getProjectTags(projectId)
    val tags: LiveData<List<TagEntry>> = tagRepository.getTags()

    val currentProject: LiveData<Project> = projectRepository.getProjectView(projectId)
    val currentPhoto: MutableLiveData<PhotoEntry?> = MutableLiveData(null)

    private var isPlaying: Boolean = false
    var fullscreenIsShowing: Boolean = false

    var lastPhoto: Photo? = null

    fun toggleSchedule(project: Project){
        viewModelScope.launch {
            var projectScheduleEntry: ProjectScheduleEntry? = projectScheduleRepository.getProjectSchedule(project.project_id)
            if (projectScheduleEntry == null)
                projectScheduleEntry = ProjectScheduleEntry(project.project_id,0, 0)
            projectScheduleRepository.setProjectSchedule(projectScheduleEntry)
        }
    }

    fun deleteTags(tags: ArrayList<String>, project: Project){
        viewModelScope.launch {
            projectTagRepository.deleteTags(tags, project)
        }
    }

    fun deleteTagFromProject(tagEntry: TagEntry, project: Project){
        viewModelScope.launch {
            projectTagRepository.deleteTagFromProject(tagEntry, project)
        }
    }

    fun deleteTagFromRepo(tagEntry: TagEntry){
        viewModelScope.launch {
            tagRepository.deleteTag(tagEntry)
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