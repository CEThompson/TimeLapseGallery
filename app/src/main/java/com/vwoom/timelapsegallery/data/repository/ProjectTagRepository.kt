package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.data.dao.ProjectTagDao
import com.vwoom.timelapsegallery.data.dao.TagDao
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project

class ProjectTagRepository private constructor(private val projectTagDao: ProjectTagDao,
                                               private val tagDao: TagDao) {

    fun getProjectTags(projectId: Long) = projectTagDao.getProjectTagsLiveDataByProjectId(projectId)

    suspend fun deleteTags(tags: ArrayList<String>, project: Project) {
        val projectTagsToDelete: ArrayList<ProjectTagEntry> = arrayListOf()

        for (tag in tags){
            // Get valid tags
            val currentTag = tagDao.getTagByText(tag)
            if (currentTag!=null) {
                // Determine if tag belongs to a project
                val projectTagEntry: ProjectTagEntry? = projectTagDao.getProjectTag(project.project_id, currentTag.id)

                if (projectTagEntry!=null) {
                    // Add tag to list for deletion
                    projectTagsToDelete.add(projectTagEntry)
                }
            }
        }

        // Bulk delete the project tags
        projectTagDao.bulkDelete(projectTagsToDelete.toList())

        // This for loop removes any tags that are not linked to projects
        // For each tag in the tags to delete
        for (tag in projectTagsToDelete){
            // Get all the project tags (links from linking table) for that tag
            val linksToProjects = projectTagDao.getProjectTagsByTagId(tag.tag_id).size
            // If there are 0 links delete the tag
            if (linksToProjects == 0) tagDao.deleteTag(tagDao.getTagById(tag.tag_id))
        }
    }

    suspend fun getProjectTags_nonLiveData(projectId: Long) = projectTagDao.getProjectTagsByProjectId(projectId)

    suspend fun addTagToProject(tagText: String, project: Project){
        var tagEntry: TagEntry? = tagDao.getTagByText(tagText)
        
        // If tag does not exist create it
        if (tagEntry == null) {
            tagEntry = TagEntry(tagText)
            tagEntry.id = tagDao.insertTag(tagEntry)
        }

        // Check if tag already belongs to project
        var projectTagEntry = projectTagDao.getProjectTag(project.project_id, tagEntry.id)

        // Only insert project tag if unique the project hasn't been assigned that tag already
        if (projectTagEntry == null) {
            projectTagEntry = ProjectTagEntry(project.project_id, tagEntry.id)
            projectTagDao.insertProjectTag(projectTagEntry)
        }
    }

    companion object {
        @Volatile private var instance: ProjectTagRepository? = null

        fun getInstance(projectTagDao: ProjectTagDao, tagDao: TagDao) =
                instance ?: synchronized(this) {
                    instance ?: ProjectTagRepository(projectTagDao, tagDao).also { instance = it }
                }
    }

}