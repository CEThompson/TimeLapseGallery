package com.vwoom.timelapsegallery.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project

class GalleryViewModel internal constructor(val repository: Repository) : ViewModel() {
    val projects: LiveData<List<Project>> = repository.getProjectViews()
    val tags: LiveData<List<TagEntry>> = repository.getTags()

    var projectFilter: List<TagEntry> = listOf()

    var filterDialogShowing = false

    fun setFilter(filter: List<TagEntry>) {
        projectFilter = filter
    }

    fun tagSelected(tag: TagEntry): Boolean {
        return projectFilter.contains(tag)
    }

    suspend fun filterProjects(projectsToFilter: List<Project>): List<Project> {
        if (projectFilter.isEmpty()) return projectsToFilter

        return projectsToFilter.filter {
            // TODO get tags for project
            val projectTags = repository.getProjectTags_nonLiveData(it.project_id)
            val tags = repository.getTagsFromProjectTags(projectTags)

            for (tag in tags){
                if (projectFilter.contains(tag)) return@filter true
            }
            false
        }
    }

    companion object {
        private val TAG = GalleryViewModel::class.java.simpleName
    }
}