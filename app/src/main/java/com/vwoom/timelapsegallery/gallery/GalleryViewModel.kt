package com.vwoom.timelapsegallery.gallery

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vwoom.timelapsegallery.data.Repository
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class GalleryViewModel internal constructor(val repository: Repository) : ViewModel() {
    val projects: LiveData<List<Project>> = repository.getProjectViews()
    val tags: LiveData<List<TagEntry>> = repository.getTags()

    var projectFilter: List<String> = listOf()

    fun setFilter(filter: List<String>) {
        projectFilter = filter
    }

    suspend fun filterProjects(projectsToFilter: List<Project>): List<Project> {
        //Log.d("tagfilter", "projects to filter are $projectsToFilter")
        if (projectFilter.isEmpty()) return projectsToFilter

        return projectsToFilter.filter {
            // TODO get tags for project
            val projectTags = repository.getProjectTags_nonLiveData(it.project_id)
            val tags = repository.getTagsFromProjectTags(projectTags)

            //Log.d("tagfilter", "tags are $tags for project ${it.project_id}")
            //Log.d(TAG, "checking $it")
            //Log.d(TAG, "tags are $tags")

            for (tag in tags){
                if (projectFilter.contains(tag.tag)) return@filter true
            }
            false
        }
        //Log.d("tagfilter", "result is $result")
    }

    companion object {
        private val TAG = GalleryViewModel::class.java.simpleName
    }
}