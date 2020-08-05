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

    @Insert
    suspend fun insertProjectTag(projectTagEntry: ProjectTagEntry)

    @Insert
    suspend fun bulkInsert(projectTags: List<ProjectTagEntry>)

    @Delete
    suspend fun bulkDelete(tagsToDelete: List<ProjectTagEntry>)

    @Delete
    suspend fun deleteProjectTag(projectTagEntry: ProjectTagEntry)

    @Update
    fun updateProjectTag(projectTagEntry: ProjectTagEntry)
}