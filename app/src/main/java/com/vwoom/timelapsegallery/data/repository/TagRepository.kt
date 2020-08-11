package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.data.dao.ProjectTagDao
import com.vwoom.timelapsegallery.data.dao.TagDao
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.ProjectView
import javax.inject.Inject

class TagRepository
@Inject constructor(private val projectTagDao: ProjectTagDao,
                    private val tagDao: TagDao) : ITagRepository {

    // Tag observables
    override suspend fun getProjectTags(projectId: Long) = projectTagDao.getProjectTagsByProjectId(projectId)
    override fun getProjectTagsLiveData(projectId: Long) = projectTagDao.getProjectTagsLiveDataByProjectId(projectId)
    override fun getTagsLiveData() = tagDao.getTagsLiveData()

    // Get the tag entries from a list of project tag entries
    override suspend fun getTagsFromProjectTags(projectTags: List<ProjectTagEntry>): List<TagEntry> {
        val tags = arrayListOf<TagEntry>()
        for (projectTag in projectTags) {
            val currentTag = tagDao.getTagById(projectTag.tag_id)
            tags.add(currentTag)
        }
        return tags
    }

    // Tag management
    override suspend fun deleteTag(tagEntry: TagEntry) {
        tagDao.deleteTag(tagEntry)
    }

    override suspend fun deleteTagFromProject(tagEntry: TagEntry, projectView: ProjectView) {
        val projectTagEntry = projectTagDao.getProjectTag(projectView.project_id, tagEntry.id)
        if (projectTagEntry != null)
            projectTagDao.deleteProjectTag(projectTagEntry)
    }

    override suspend fun addTagToProject(tagText: String, projectView: ProjectView) {
        var tagEntry: TagEntry? = tagDao.getTagByText(tagText)
        // If tag does not exist create it
        if (tagEntry == null) {
            tagEntry = TagEntry(tagText)
            tagEntry.id = tagDao.insertTag(tagEntry)
        }

        // Check if tag already belongs to project
        var projectTagEntry = projectTagDao.getProjectTag(projectView.project_id, tagEntry.id)

        // Only insert project tag if the project hasn't been assigned that tag already
        if (projectTagEntry == null) {
            projectTagEntry = ProjectTagEntry(projectView.project_id, tagEntry.id)
            projectTagDao.insertProjectTag(projectTagEntry)
        }
    }

    companion object {
        @Volatile
        private var instance: TagRepository? = null

        fun getInstance(projectTagDao: ProjectTagDao, tagDao: TagDao) =
                instance ?: synchronized(this) {
                    instance ?: TagRepository(projectTagDao, tagDao).also { instance = it }
                }
    }
}