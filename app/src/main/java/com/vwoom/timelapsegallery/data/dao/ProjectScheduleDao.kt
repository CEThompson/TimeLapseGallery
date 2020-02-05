package com.vwoom.timelapsegallery.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry

@Dao
interface ProjectScheduleDao {
    @Query("SELECT * FROM project_schedule")
    fun getProjectSchedulesLiveData(): LiveData<List<ProjectScheduleEntry>>

    @Query("SELECT * FROM project_schedule")
    fun getProjectSchedules(): List<ProjectScheduleEntry>

    @Query("SELECT * FROM project_schedule WHERE project_id = :projectId")
    suspend fun getProjectScheduleByProjectId(projectId: Long): ProjectScheduleEntry?

    // TODO reconsider work manager and remote views usage of this function
    @Query("SELECT * FROM project_schedule WHERE project_id = :projectId")
    fun getProjectScheduleByProjectIdNonSuspend(projectId: Long): ProjectScheduleEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectSchedule(projectScheduleEntry: ProjectScheduleEntry)

    @Delete
    suspend fun deleteProjectSchedule(projectScheduleEntry: ProjectScheduleEntry)

    @Update
    suspend fun updateProjectSchedule(projectScheduleEntry: ProjectScheduleEntry)
}