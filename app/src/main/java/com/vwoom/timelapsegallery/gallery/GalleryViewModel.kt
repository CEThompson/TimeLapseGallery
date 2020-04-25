package com.vwoom.timelapsegallery.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.repository.TagRepository
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.utils.TimeUtils.daysUntilDue
import java.util.*
import javax.inject.Inject

const val SEARCH_TYPE_NONE = "none"
const val SEARCH_TYPE_DUE_TODAY = "due_today"
const val SEARCH_TYPE_DUE_TOMORROW = "due_tomorrow"
const val SEARCH_TYPE_PENDING = "pending"
const val SEARCH_TYPE_SCHEDULED = "scheduled"
const val SEARCH_TYPE_UNSCHEDULED = "unscheduled"

class GalleryViewModel
@Inject constructor (projectRepository: ProjectRepository, private val tagRepository: TagRepository) : ViewModel() {
    // Tag Live Data
    val projects: LiveData<List<ProjectView>> = projectRepository.getProjectViewsLiveData()
    val tags: LiveData<List<TagEntry>> = tagRepository.getTagsLiveData()

    // Inputted search data
    var searchTags: ArrayList<TagEntry> = arrayListOf()
    var searchName: String = ""
    var searchType: String = SEARCH_TYPE_NONE

    // The resulting projects
    var displayedProjectViews: List<ProjectView> = listOf()

    // State of user interaction
    var searchDialogShowing = false
    var userClickedToStopSearch = false

    // Returns whether or not a tag was selected by the user
    fun tagSelected(tag: TagEntry): Boolean {
        return searchTags.contains(tag)
    }

    // Filters the projects by the inputted search parameters
    suspend fun filterProjects(): List<ProjectView> {
        if (projects.value == null) return listOf()
        var resultProjects = projects.value!!

        if (searchTags.isNotEmpty()) {
            resultProjects = resultProjects.filter {
                val projectTags: List<ProjectTagEntry> = tagRepository.getProjectTags(it.project_id)
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
                if (it.project_name.toLowerCase(Locale.getDefault()).contains(searchName.toLowerCase(Locale.getDefault()))) return@filter true
                return@filter false
            }
        }

        when (searchType) {
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