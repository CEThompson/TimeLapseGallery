package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.data.dao.TagDao
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry

class TagRepository private constructor(private val tagDao: TagDao){

    fun getTags() = tagDao.getTagsLiveData()

    suspend fun getTagsFromProjectTags(projectTags: List<ProjectTagEntry>): List<TagEntry> {
        val tags = arrayListOf<TagEntry>()
        for (projectTag in projectTags){
            val currentTag = tagDao.getTagById(projectTag.tag_id)
            tags.add(currentTag)
        }
        return tags
    }

    companion object {
        @Volatile private var instance: TagRepository? = null

        fun getInstance(tagDao: TagDao) =
                instance ?: synchronized(this) {
                    instance ?: TagRepository(tagDao).also { instance = it }
                }
    }
}