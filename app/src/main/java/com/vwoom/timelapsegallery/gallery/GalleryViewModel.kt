package com.vwoom.timelapsegallery.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.repository.ProjectTagRepository
import com.vwoom.timelapsegallery.data.repository.TagRepository
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.utils.TimeUtils

const val SEARCH_TYPE_NONE = "none"
const val SEARCH_TYPE_DUE = "due"
const val SEARCH_TYPE_PENDING = "pending"
const val SEARCH_TYPE_SCHEDULED = "scheduled"
const val SEARCH_TYPE_UNSCHEDULED = "unscheduled"

class GalleryViewModel internal constructor(private val projectRepository: ProjectRepository,
                                            private val tagRepository: TagRepository,
                                            private val projectTagRepository: ProjectTagRepository) : ViewModel() {
    val projects: LiveData<List<Project>> = projectRepository.getProjectViews()
    val tags: LiveData<List<TagEntry>> = tagRepository.getTags()

    // search data
    var searchTags: ArrayList<TagEntry> = arrayListOf()
    var searchName: String = ""
    var searchType: String = SEARCH_TYPE_NONE

    var displayedProjects: List<Project> = listOf()

    var searchDialogShowing = false

    fun tagSelected(tag: TagEntry): Boolean {
        return searchTags.contains(tag)
    }

    suspend fun filterProjects(): List<Project> {
        if (projects.value == null) return listOf()
        var resultProjects = projects.value!!

        if (searchTags.isNotEmpty()) {
            resultProjects = resultProjects.filter {
                val projectTags: List<ProjectTagEntry> = projectTagRepository.getProjectTags_nonLiveData(it.project_id)
                val tagEntriesForProject: List<TagEntry> = tagRepository.getTagsFromProjectTags(projectTags)

                for (tag in searchTags)
                    if (tagEntriesForProject.contains(tag)) return@filter true

                return@filter false
            }
        }

        if (searchName.isNotEmpty()) {
            resultProjects = resultProjects.filter {
                if (it.project_name == null) return@filter false
                if (it.project_name!!.contains(searchName)) return@filter true
                return@filter false
            }
        }

        // TODO simplify search
        when(searchType) {
            SEARCH_TYPE_SCHEDULED -> {
                resultProjects = resultProjects.filter {
                    if (it.interval_days == null) return@filter false
                    if (it.interval_days == 0) return@filter false
                    return@filter true
                }
            }
            SEARCH_TYPE_UNSCHEDULED -> {
                resultProjects = resultProjects.filter {
                    if (it.interval_days == null) return@filter true
                    if (it.interval_days == 0) return@filter true
                    return@filter false
                }
            }
            SEARCH_TYPE_DUE -> {
                resultProjects = resultProjects.filter {
                    if (it.interval_days == null || it.interval_days == 0) return@filter false
                    val daysSinceLastPhotoTaken = TimeUtils.getDaysSinceTimeStamp(it.cover_photo_timestamp)
                    val interval: Int = it.interval_days
                    val daysUntilDue = interval - daysSinceLastPhotoTaken
                    return@filter daysUntilDue <= 0
                }
            }
            SEARCH_TYPE_PENDING -> {
                resultProjects = resultProjects.filter {
                    if (it.interval_days == null || it.interval_days == 0) return@filter false
                    val daysSinceLastPhotoTaken = TimeUtils.getDaysSinceTimeStamp(it.cover_photo_timestamp)
                    val interval: Int = it.interval_days
                    val daysUntilDue = interval - daysSinceLastPhotoTaken
                    return@filter daysUntilDue > 0
                }
            }
        }
        return resultProjects
    }

    companion object {
        private val TAG = GalleryViewModel::class.java.simpleName
    }
}