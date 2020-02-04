package com.vwoom.timelapsegallery.gallery

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
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

    var projectFilter: List<TagEntry> = listOf()

    var searchName: String? = null
    var todaySearch: Boolean = false
    var scheduleSearch: Boolean = false
    var unscheduledSearch: Boolean = false

    var returnPosition: Int = 0

    var filterDialogShowing = false

    fun setFilter(filter: List<TagEntry>, name: String?, today: Boolean, scheduled: Boolean, unscheduled: Boolean) {
        projectFilter = filter
        searchName = name
        todaySearch = today
        scheduleSearch = scheduled
        unscheduledSearch = unscheduled
    }

    fun tagSelected(tag: TagEntry): Boolean {
        return projectFilter.contains(tag)
    }

    suspend fun filterProjects(projectsToFilter: List<Project>): List<Project> {
        var result = projectsToFilter

        Log.d(TAG, "initial $result")
        if (projectFilter.isNotEmpty()) {
            result = projectsToFilter.filter {
                val projectTags = projectTagRepository.getProjectTags_nonLiveData(it.project_id)
                val tags = tagRepository.getTagsFromProjectTags(projectTags)

                for (tag in tags) {
                    if (projectFilter.contains(tag)) return@filter true
                }
                false
            }
        }
        Log.d(TAG, "after tag filter $result")
        if (!searchName.isNullOrEmpty()) {
            val projectsByName = convertEntriesToProjects(projectRepository.getProjectsByName(searchName!!))
            result = result.intersect(projectsByName).toList()
        }
        Log.d(TAG, "search term is $searchName")
        Log.d(TAG, "after name filter $result")
        if (todaySearch){
            val projectsScheduledToday = convertEntriesToProjects(projectRepository.getScheduledProjects())
            result = result.intersect(projectsScheduledToday).toList()
        }
        Log.d(TAG, "after today filter $result")
        if (scheduleSearch){
            val scheduledProjects = convertEntriesToProjects(projectRepository.getScheduledProjects())
            result = result.intersect(scheduledProjects).toList()
        }
        Log.d(TAG, "after schedule filter $result")
        if (unscheduledSearch){
            val unscheduledProjects = convertEntriesToProjects(projectRepository.getUnscheduledProjects())
            result = result.intersect(unscheduledProjects).toList()
        }
        Log.d(TAG, "after unscheduled filter $result")
        return result
    }

    // TODO refactor this
    private suspend fun convertEntriesToProjects(entries: List<ProjectEntry>): List<Project>{
        val projects = arrayListOf<Project>()
        for (entry in entries){
            projects.add(projectRepository.getProjectViewById(entry.id))
        }
        return projects
    }

    companion object {
        private val TAG = GalleryViewModel::class.java.simpleName
    }
}