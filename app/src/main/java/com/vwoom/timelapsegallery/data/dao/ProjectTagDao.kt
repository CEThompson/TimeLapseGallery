package com.vwoom.timelapsegallery.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry

@Dao
interface ProjectTagDao {
    @Query("SELECT * FROM project_tag WHERE project_id = :projectId")
    fun loadTagsByProjectId(projectId: Long): LiveData<List<ProjectTagEntry>>

    @Query("SELECT * FROM project_tag WHERE project_id = :projectId")
    suspend fun loadTagsByProjectId_nonLiveData(projectId: Long): List<ProjectTagEntry>

    @Insert
    suspend fun insertProjectTag(projectTagEntry: ProjectTagEntry)

    @Delete
    fun deleteProjectTag(projectTagEntry: ProjectTagEntry)

    @Update
    fun updateProjectTag(projectTagEntry: ProjectTagEntry)
}