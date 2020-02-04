package com.vwoom.timelapsegallery.data.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.view.Project

@Dao
interface ProjectDao {
    @Query("SELECT * FROM project ORDER BY id")
    fun getProjectsLiveData(): LiveData<List<ProjectEntry>>

    @Query("SELECT * FROM project " +
            "INNER JOIN project_schedule " +
            "ON project.id = project_schedule.project_id " +
            "WHERE project_schedule.interval_days != 0")
    fun getScheduledProjects(): List<ProjectEntry>

    @Query("SELECT * FROM project " +
            "INNER JOIN project_schedule " +
            "ON project.id = project_schedule.project_id " +
            "WHERE project_schedule.interval_days = 0")
    fun getUnscheduledProjects(): List<ProjectEntry>

    @Query("SELECT * FROM project " +
            "WHERE project_name LIKE '%' || :search || '%'")
    suspend fun getProjectsByName(search: String): List<ProjectEntry>

    @Query("SELECT * FROM project WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntry

    @Query("SELECT * FROM project WHERE id = :id")
    fun getProjectLiveDataById(id: Long): LiveData<ProjectEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(projectEntry: ProjectEntry): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProject(projectEntry: ProjectEntry)

    @Delete
    suspend fun deleteProjectByEntry(projectEntry: ProjectEntry)

    @Query("DELETE FROM project")
    suspend fun deleteAllProjects()

    /* Returns a livedata object for observing projects view */
    @Query("SELECT " +
            "project.id AS project_id, " +
            "project.project_name AS project_name, " +
            "project_schedule.schedule_time AS schedule_time, " +
            "project_schedule.interval_days AS interval_days, " +
            "cover_photo.photo_id AS cover_photo_id, " +
            "photo.timestamp AS cover_photo_timestamp " +
            "FROM project " +
            "LEFT JOIN project_schedule ON project.id = project_schedule.project_id " +
            "LEFT JOIN cover_photo ON project.id = cover_photo.project_id " +
            "LEFT JOIN photo ON cover_photo.photo_id = photo.id")
    fun getProjectViewsLiveData(): LiveData<List<Project>>

    @Query("SELECT " +
            "project.id AS project_id, " +
            "project.project_name AS project_name, " +
            "project_schedule.schedule_time AS schedule_time, " +
            "project_schedule.interval_days AS interval_days, " +
            "cover_photo.photo_id AS cover_photo_id, " +
            "photo.timestamp AS cover_photo_timestamp " +
            "FROM project " +
            "LEFT JOIN project_schedule ON project.id = project_schedule.project_id " +
            "LEFT JOIN cover_photo ON project.id = cover_photo.project_id " +
            "LEFT JOIN photo ON cover_photo.photo_id = photo.id " +
            "WHERE project.id =:id")
    fun getProjectViewLiveData(id: Long): LiveData<Project>

    @Query("SELECT " +
            "project.id AS project_id, " +
            "project.project_name AS project_name, " +
            "project_schedule.schedule_time AS schedule_time, " +
            "project_schedule.interval_days AS interval_days, " +
            "cover_photo.photo_id AS cover_photo_id, " +
            "photo.timestamp AS cover_photo_timestamp " +
            "FROM project " +
            "LEFT JOIN project_schedule ON project.id = project_schedule.project_id " +
            "LEFT JOIN cover_photo ON project.id = cover_photo.project_id " +
            "LEFT JOIN photo ON cover_photo.photo_id = photo.id " +
            "WHERE project.id =:id")
    suspend fun getProjectViewById(id: Long): Project
}