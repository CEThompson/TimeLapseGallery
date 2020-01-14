package com.vwoom.timelapsegallery.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.repository.ProjectTagRepository
import com.vwoom.timelapsegallery.data.repository.TagRepository
import com.vwoom.timelapsegallery.data.view.Project

class GalleryViewModel internal constructor(val projectRepository: ProjectRepository,
                                            val tagRepository: TagRepository,
                                            val projectTagRepository: ProjectTagRepository) : ViewModel() {
    val projects: LiveData<List<Project>> = projectRepository.getProjectViews()
    val tags: LiveData<List<TagEntry>> = tagRepository.getTags()

    var projectFilter: List<TagEntry> = listOf()
    var returnPosition: Int = 0

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
            val projectTags = projectTagRepository.getProjectTags_nonLiveData(it.project_id)
            val tags = tagRepository.getTagsFromProjectTags(projectTags)

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