package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.data.dao.ProjectTagDao
import com.vwoom.timelapsegallery.data.dao.TagDao
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project

class ProjectTagRepository private constructor(private val projectTagDao: ProjectTagDao,
                                               private val tagDao: TagDao) {

    fun getProjectTagsLiveData(projectId: Long) = projectTagDao.getProjectTagsLiveDataByProjectId(projectId)

    suspend fun getProjectTags(projectId: Long) = projectTagDao.getProjectTagsByProjectId(projectId)

    suspend fun deleteTagFromProject(tagEntry: TagEntry, project: Project) {
        val projectTagEntry = projectTagDao.getProjectTag(project.project_id, tagEntry.id)
        if (projectTagEntry != null)
            projectTagDao.deleteProjectTag(projectTagEntry)
    }

    suspend fun addTagToProject(tagText: String, project: Project) {
        var tagEntry: TagEntry? = tagDao.getTagByText(tagText)

        // If tag does not exist create it
        if (tagEntry == null) {
            tagEntry = TagEntry(tagText)
            tagEntry.id = tagDao.insertTag(tagEntry)
        }

        // Check if tag already belongs to project
        var projectTagEntry = projectTagDao.getProjectTag(project.project_id, tagEntry.id)

        // Only insert project tag if the project hasn't been assigned that tag already
        if (projectTagEntry == null) {
            projectTagEntry = ProjectTagEntry(project.project_id, tagEntry.id)
            projectTagDao.insertProjectTag(projectTagEntry)
        }
    }

    companion object {
        @Volatile
        private var instance: ProjectTagRepository? = null

        fun getInstance(projectTagDao: ProjectTagDao, tagDao: TagDao) =
                instance ?: synchronized(this) {
                    instance ?: ProjectTagRepository(projectTagDao, tagDao).also { instance = it }
                }
    }
}