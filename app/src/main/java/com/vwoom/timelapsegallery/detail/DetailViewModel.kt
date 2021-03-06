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
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.gif.GifUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils.getProjectEntryFromProjectView
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject
import kotlin.properties.Delegates

class DetailViewModel @Inject constructor(
        private val projectRepository: ProjectRepository,
        private val tagRepository: TagRepository) : ViewModel() {

    private var projectId by Delegates.notNull<Long>()

    // Live data
    lateinit var projectView: LiveData<ProjectView?>
    lateinit var photos: LiveData<List<PhotoEntry>>
    lateinit var projectTags: LiveData<List<ProjectTagEntry>>
    lateinit var tags: LiveData<List<TagEntry>>

    // Dialog state
    var scheduleDialogShowing: Boolean = false
    var infoDialogShowing: Boolean = false
    var tagDialogShowing: Boolean = false
    var convertDialogShowing: Boolean = false

    // Photo state and positioning
    val currentPhoto: MutableLiveData<PhotoEntry?> = MutableLiveData(null)
    var lastPhoto: Photo? = null    // used to pass to the camera fragment
    var photoIndex: Int = 0
    var maxIndex: Int = Integer.MAX_VALUE

    // For setting project info after viewModel creation
    fun setProject(id: Long) {
        projectId = id
        projectView = projectRepository.getProjectViewLiveData(projectId)
        photos = projectRepository.getProjectPhotosLiveData(projectId)
        projectTags = tagRepository.getProjectTagsLiveData(projectId)
        tags = tagRepository.getTagsLiveData()
    }

    fun setSchedule(externalFilesDir: File, projectView: ProjectView, intervalInDays: Int) {
        viewModelScope.launch {
            val projectScheduleEntry = ProjectScheduleEntry(projectView.project_id, intervalInDays)
            projectRepository.setProjectSchedule(externalFilesDir, projectView, projectScheduleEntry)
        }
    }

    fun deleteTagFromProject(tagEntry: TagEntry, projectView: ProjectView) {
        viewModelScope.launch {
            tagRepository.deleteTagFromProject(tagEntry, projectView)
        }
    }

    fun deleteTagFromDatabase(tagEntry: TagEntry) {
        viewModelScope.launch {
            tagRepository.deleteTag(tagEntry)
        }
    }

    fun updateProjectName(externalFilesDir: File, name: String, source: ProjectView) {
        viewModelScope.launch {
            projectRepository.updateProjectName(externalFilesDir, source, name)
        }
    }

    // For passing to the camera fragment
    fun setLastPhotoByEntry(externalFilesDir: File, projectView: ProjectView, entry: PhotoEntry) {
        val url = ProjectUtils.getProjectPhotoUrl(
                externalFilesDir,
                getProjectEntryFromProjectView(projectView),
                entry.timestamp) ?: return
        lastPhoto = Photo(entry.project_id, entry.id, entry.timestamp, url)
    }

    fun nextPhoto() {
        if (photos.value == null || photos.value!!.size <= 1) return
        if (photoIndex == maxIndex) return
        photoIndex += 1
        currentPhoto.value = photos.value!![photoIndex]
    }

    fun previousPhoto() {
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

    fun addTag(tagText: String, projectView: ProjectView) {
        viewModelScope.launch {
            tagRepository.addTagToProject(tagText, projectView)
        }
    }

    fun getTags(projectTags: List<ProjectTagEntry>): List<TagEntry> = runBlocking {
        tagRepository.getTagsFromProjectTags(projectTags)
    }

    fun deleteCurrentPhoto(externalFilesDir: File) {
        viewModelScope.launch {
            // First get the next photo entry to display. If no photo entry do nothing
            val currentPhotoEntry: PhotoEntry = currentPhoto.value ?: return@launch

            // If photo was the last set the new index to the previous
            if (photoIndex == maxIndex) photoIndex--

            // Delete the current entry
            projectRepository.deleteProjectPhoto(externalFilesDir, currentPhotoEntry)
        }
    }

    fun deleteCurrentProject(externalFilesDir: File, project: ProjectView) {
        viewModelScope.launch {
            projectRepository.deleteProject(externalFilesDir, projectId)
            GifUtils.deleteGif(externalFilesDir, getProjectEntryFromProjectView(project))
        }
    }

}