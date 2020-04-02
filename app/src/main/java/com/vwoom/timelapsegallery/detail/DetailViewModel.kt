package com.vwoom.timelapsegallery.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.repository.TagRepository
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils.getProjectEntryFromProjectView
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class DetailViewModel(private val projectRepository: ProjectRepository,
                      private val tagRepository: TagRepository,
                      projectId: Long) : ViewModel() {
    // Project & Photo
    val currentProject: LiveData<Project> = projectRepository.getProjectViewLiveData(projectId)
    val currentPhoto: MutableLiveData<PhotoEntry?> = MutableLiveData(null)
    var lastPhoto: Photo? = null
    val photos: LiveData<List<PhotoEntry>> = projectRepository.getProjectPhotosLiveData(projectId)
    val projectTags: LiveData<List<ProjectTagEntry>> = tagRepository.getProjectTagsLiveData(projectId)
    val tags: LiveData<List<TagEntry>> = tagRepository.getTagsLiveData()

    // Dialog state
    var fullscreenDialogShowing: Boolean = false
    var scheduleDialogShowing: Boolean = false
    var infoDialogShowing: Boolean = false
    var tagDialogShowing: Boolean = false

    // Play state
    private var isPlaying: Boolean = false

    var photoIndex: Int = 0
    var maxIndex: Int = 0

    fun setSchedule(externalFilesDir: File, project: Project, intervalInDays: Int) {
        viewModelScope.launch {
            val projectScheduleEntry = ProjectScheduleEntry(project.project_id, intervalInDays)
            projectRepository.setProjectSchedule(externalFilesDir, project, projectScheduleEntry)
        }
    }

    fun deleteTagFromProject(tagEntry: TagEntry, project: Project) {
        viewModelScope.launch {
            tagRepository.deleteTagFromProject(tagEntry, project)
        }
    }

    fun deleteTagFromRepo(tagEntry: TagEntry) {
        viewModelScope.launch {
            tagRepository.deleteTag(tagEntry)
        }
    }

    fun updateProjectName(externalFilesDir: File, name: String, source: Project) {
        viewModelScope.launch {
            projectRepository.updateProjectName(externalFilesDir, source, name)
        }
    }

    fun setLastPhotoByEntry(externalFilesDir: File, project: Project, entry: PhotoEntry) {
        val url = FileUtils.getPhotoUrl(
                externalFilesDir,
                getProjectEntryFromProjectView(project),
                entry.timestamp)
        lastPhoto = Photo(entry.project_id, entry.id, entry.timestamp, url)
    }

    fun nextPhoto() {
        if (isPlaying) return
        if (photos.value == null || photos.value!!.size <= 1) return
        if (photoIndex == maxIndex) return
        photoIndex += 1
        currentPhoto.value = photos.value!![photoIndex]
    }

    fun previousPhoto() {
        if (isPlaying) return
        if (photos.value == null || photos.value!!.isEmpty()) return
        if (photoIndex == 0) return
        photoIndex -= 1
        currentPhoto.value = photos.value!![photoIndex]
    }

    fun setPhoto(photoEntry: PhotoEntry) {
        currentPhoto.value = photoEntry
        photoIndex = photos.value!!.indexOf(photoEntry)
    }

    fun setCoverPhoto(photoEntry: PhotoEntry) {
        viewModelScope.launch {
            projectRepository.setProjectCoverPhoto(photoEntry)
        }
    }

    fun addTag(tagText: String, project: Project) {
        viewModelScope.launch {
            tagRepository.addTagToProject(tagText, project)
        }
    }

    fun getTags(projectTags: List<ProjectTagEntry>): List<TagEntry> = runBlocking {
        tagRepository.getTagsFromProjectTags(projectTags)
    }

    fun deleteCurrentPhoto(externalFilesDir: File) {
        viewModelScope.launch {
            // First get the next photo entry to display
            val currentPhotoEntry: PhotoEntry = currentPhoto.value ?: return@launch

            if (photoIndex == maxIndex) photoIndex--

            // Delete the current entry
            projectRepository.deleteProjectPhoto(externalFilesDir, currentPhotoEntry)

        }
    }

    fun deleteCurrentProject(externalFilesDir: File) {
        viewModelScope.launch {
            projectRepository.deleteProject(externalFilesDir, currentProject.value?.project_id!!)
        }
    }
}