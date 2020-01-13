package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.data.dao.ProjectTagDao
import com.vwoom.timelapsegallery.data.dao.TagDao
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project

class ProjectTagRepository private constructor(val projectTagDao: ProjectTagDao, val tagDao: TagDao) {

    fun getProjectTags(projectId: Long) = projectTagDao.loadTagsByProjectId(projectId)

    suspend fun getProjectTags_nonLiveData(projectId: Long) = projectTagDao.loadTagsByProjectId_nonLiveData(projectId)

    suspend fun addTagToProject(tagText: String, project: Project){
        var tagEntry: TagEntry? = tagDao.loadTagByText(tagText)

        // If tag does not exist create a new entry
        if (tagEntry == null) tagEntry = TagEntry(tagText)

        val tagId = tagDao.insertTag(tagEntry)
        val projectTagEntry = ProjectTagEntry(project.project_id, tagId)
        projectTagDao.insertProjectTag(projectTagEntry)
    }

    companion object {
        @Volatile private var instance: ProjectTagRepository? = null

        fun getInstance(projectTagDao: ProjectTagDao, tagDao: TagDao) =
                instance ?: synchronized(this) {
                    instance ?: ProjectTagRepository(projectTagDao, tagDao).also { instance = it }
                }
    }

}