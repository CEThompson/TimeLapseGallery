package com.vwoom.timelapsegallery.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry

@Dao
interface ProjectTagDao {
    @Query("SELECT * FROM project_tag")
    fun getProjectTags(): List<ProjectTagEntry>

    @Query("SELECT * FROM project_tag WHERE project_id = :projectId")
    fun getProjectTagsLiveDataByProjectId(projectId: Long): LiveData<List<ProjectTagEntry>>

    @Query("SELECT * FROM project_tag WHERE project_id = :projectId")
    suspend fun getProjectTagsByProjectId(projectId: Long): List<ProjectTagEntry>

    @Query("SELECT * FROM project_tag WHERE project_id = :projectId AND tag_id = :tagId")
    suspend fun getProjectTag(projectId: Long, tagId: Long): ProjectTagEntry?

    @Query("SELECT * FROM project_tag WHERE tag_id = :tagId")
    suspend fun getProjectTagsByTagId(tagId: Long): List<ProjectTagEntry>

    @Insert
    suspend fun insertProjectTag(projectTagEntry: ProjectTagEntry)

    @Delete
    suspend fun bulkDelete(tagsToDelete: List<ProjectTagEntry>)

    @Delete
    suspend fun deleteProjectTag(projectTagEntry: ProjectTagEntry)

    @Update
    fun updateProjectTag(projectTagEntry: ProjectTagEntry)
}