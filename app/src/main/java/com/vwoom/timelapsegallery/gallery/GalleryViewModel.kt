package com.vwoom.timelapsegallery.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.repository.ProjectTagRepository
import com.vwoom.timelapsegallery.data.repository.TagRepository
import com.vwoom.timelapsegallery.data.view.Project

class GalleryViewModel internal constructor(private val projectRepository: ProjectRepository,
                                            private val tagRepository: TagRepository,
                                            private val projectTagRepository: ProjectTagRepository) : ViewModel() {
    val projects: LiveData<List<Project>> = projectRepository.getProjectViews()
    val tags: LiveData<List<TagEntry>> = tagRepository.getTags()

    // search data
    var searchTags: ArrayList<TagEntry> = arrayListOf()
    var searchName: String = ""
    var scheduleSearch: Boolean = false
    var unscheduledSearch: Boolean = false

    var allProjects: List<Project> = listOf()
    var currentProjects: List<Project> = listOf()

    var returnPosition: Int = 0

    var searchDialogShowing = false

    fun tagSelected(tag: TagEntry): Boolean {
        return searchTags.contains(tag)
    }

    suspend fun filterProjects(): List<Project> {
        var resultProjects = allProjects

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

        if (scheduleSearch){
            resultProjects = resultProjects.filter {
                if (it.interval_days == null) return@filter false
                if (it.interval_days == 0) return@filter false
                return@filter true
            }
        } else if (unscheduledSearch) {
            resultProjects = resultProjects.filter {
                if (it.interval_days == null) return@filter true
                if (it.interval_days == 0) return@filter true
                return@filter false
            }
        }
        return resultProjects
    }

    companion object {
        private val TAG = GalleryViewModel::class.java.simpleName
    }
}