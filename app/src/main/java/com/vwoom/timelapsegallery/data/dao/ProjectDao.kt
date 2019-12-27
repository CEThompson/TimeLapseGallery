package com.vwoom.timelapsegallery.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.view.Project

@Dao
interface ProjectDao {
    @Query("SELECT * FROM project ORDER BY id")
    fun loadAllProjects(): LiveData<List<ProjectEntry>>

    // TODO test this join query
    //@Query("SELECT * FROM project WHERE schedule != 0 ORDER BY schedule_next_submission")
    @Query("SELECT * FROM project " +
            "INNER JOIN project_schedule " +
            "ON project.id = project_schedule.project_id " +
            "ORDER BY project_schedule.schedule_time")
    fun loadAllScheduledProjects(): List<ProjectEntry>

    @Query("SELECT * FROM project WHERE id = :id")
    fun loadProjectById(id: Long): ProjectEntry?

    @Query("SELECT * FROM project WHERE id = :id")
    fun loadLiveDataProjectById(id: Long): LiveData<ProjectEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(projectEntry: ProjectEntry): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProject(projectEntry: ProjectEntry)

    @Delete
    suspend fun deleteProject(projectEntry: ProjectEntry)

    // TODO test delete by project id
    @Query ("DELETE FROM project WHERE project.id = :projectId")
    suspend fun deleteProject(projectId: Long)

    @Query("DELETE FROM project")
    suspend fun deleteAllProjects()

    /* Returns a livedata object for observing projects view */
    @Query("SELECT " +
            "project.id AS project_id, " +
            "project.project_name AS project_name, " +
            "project.cover_set_by_user AS cover_set_by_user, " +
            "project_schedule.schedule_time AS schedule_time, " +
            "project_schedule.interval_days AS interval_days, " +
            "cover_photo.photo_id AS cover_photo_id, " +
            "photo.timestamp AS cover_photo_timestamp " +
            "FROM project " +
            "LEFT JOIN project_schedule ON project.id = project_schedule.project_id " +
            "LEFT JOIN cover_photo ON project.id = cover_photo.project_id " +
            "LEFT JOIN photo ON cover_photo.photo_id = photo.id")
    fun loadProjectViews(): LiveData<List<Project>>

    @Query("SELECT " +
            "project.id AS project_id, " +
            "project.project_name AS project_name, " +
            "project.cover_set_by_user AS cover_set_by_user, " +
            "project_schedule.schedule_time AS schedule_time, " +
            "project_schedule.interval_days AS interval_days, " +
            "cover_photo.photo_id AS cover_photo_id, " +
            "photo.timestamp AS cover_photo_timestamp " +
            "FROM project " +
            "LEFT JOIN project_schedule ON project.id = project_schedule.project_id " +
            "LEFT JOIN cover_photo ON project.id = cover_photo.project_id " +
            "LEFT JOIN photo ON cover_photo.photo_id = photo.id " +
            "WHERE project.id =:id")
    fun loadProjectView(id: Long): LiveData<Project>
}