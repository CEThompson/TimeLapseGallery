package com.vwoom.timelapsegallery.data.repository

import androidx.lifecycle.LiveData
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.ProjectView

interface ITagRepository {
    // Tag observables
    suspend fun getProjectTags(projectId: Long): List<ProjectTagEntry>
    fun getProjectTagsLiveData(projectId: Long): LiveData<List<ProjectTagEntry>>
    fun getTagsLiveData(): LiveData<List<TagEntry>>

    // Get the tag entries from a list of project tag entries
    suspend fun getTagsFromProjectTags(projectTags: List<ProjectTagEntry>): List<TagEntry>

    // Tag management
    suspend fun deleteTag(tagEntry: TagEntry)
    suspend fun deleteTagFromProject(tagEntry: TagEntry, projectView: ProjectView)
    suspend fun addTagToProject(tagText: String, projectView: ProjectView)
}