package com.vwoom.timelapsegallery.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vwoom.timelapsegallery.database.entry.ProjectScheduleEntry

@Dao
interface ProjectScheduleDao {
    @Query("SELECT * FROM project_schedule")
    fun loadProjectSchedules(): LiveData<List<ProjectScheduleEntry?>?>?

    @Query("SELECT * FROM project_schedule WHERE project_id = :projectId")
    fun loadScheduleByProjectId(projectId: Long): ProjectScheduleEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProjectSchedule(projectScheduleEntry: ProjectScheduleEntry?)

    @Delete
    fun deleteProjectSchedule(projectScheduleEntry: ProjectScheduleEntry?)

    @Update
    fun updateProjectSchedule(projectScheduleEntry: ProjectScheduleEntry?)
}