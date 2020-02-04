package com.vwoom.timelapsegallery.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry

@Dao
interface ProjectScheduleDao {
    @Query("SELECT * FROM project_schedule")
    fun getProjectSchedules(): LiveData<List<ProjectScheduleEntry>>

    @Query("SELECT * FROM project_schedule WHERE project_id = :projectId")
    fun getProjectScheduleByProjectId(projectId: Long): ProjectScheduleEntry

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectSchedule(projectScheduleEntry: ProjectScheduleEntry)

    @Delete
    suspend fun deleteProjectSchedule(projectScheduleEntry: ProjectScheduleEntry)

    @Update
    suspend fun updateProjectSchedule(projectScheduleEntry: ProjectScheduleEntry)
}