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
import com.vwoom.timelapsegallery.utils.TimeUtils.daysUntilDue

const val SEARCH_TYPE_NONE = "none"
const val SEARCH_TYPE_DUE_TODAY = "due_today"
const val SEARCH_TYPE_DUE_TOMORROW = "due_tomorrow"
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

    var userClickedToStopSearch = false

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

                // Include projects with tags included in the search filter
                for (tag in searchTags)
                    if (tagEntriesForProject.contains(tag)) return@filter true

                return@filter false
            }
        }

        if (searchName.isNotEmpty()) {
            resultProjects = resultProjects.filter {
                if (it.project_name == null) return@filter false
                if (it.project_name.contains(searchName)) return@filter true
                return@filter false
            }
        }

        when(searchType) {
            SEARCH_TYPE_SCHEDULED -> {
                resultProjects = resultProjects.filter {
                    return@filter it.interval_days > 0
                }
            }
            SEARCH_TYPE_UNSCHEDULED -> {
                resultProjects = resultProjects.filter {
                    return@filter it.interval_days == 0
                }
            }
            SEARCH_TYPE_DUE_TODAY -> {
                resultProjects = resultProjects.filter {
                    if (it.interval_days == 0) return@filter false
                    return@filter daysUntilDue(it) <= 0
                }
            }
            SEARCH_TYPE_DUE_TOMORROW -> {
                resultProjects = resultProjects.filter {
                    if (it.interval_days == 0) return@filter false
                    return@filter daysUntilDue(it) == 1.toLong()
                }
            }
            SEARCH_TYPE_PENDING -> {
                resultProjects = resultProjects.filter {
                    if (it.interval_days == 0) return@filter false
                    return@filter daysUntilDue(it) > 0
                }
                resultProjects = resultProjects.sortedBy { daysUntilDue(it) } // show projects due earlier first
            }
        }
        return resultProjects
    }

    companion object {
        private val TAG = GalleryViewModel::class.java.simpleName
    }
}