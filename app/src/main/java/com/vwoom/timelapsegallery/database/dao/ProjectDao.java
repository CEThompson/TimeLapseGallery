package com.vwoom.timelapsegallery.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.database.view.Project;

import java.util.List;

@Dao
public interface ProjectDao {

    @Query("SELECT * FROM project ORDER BY id")
    LiveData<List<ProjectEntry>> loadAllProjects();

    // TODO test this join query
    //@Query("SELECT * FROM project WHERE schedule != 0 ORDER BY schedule_next_submission")
    @Query("SELECT * FROM project " +
            "INNER JOIN project_schedule " +
            "ON project.id = project_schedule.project_id " +
            "ORDER BY project_schedule.schedule_time")
    List<ProjectEntry> loadAllScheduledProjects();

    @Query("SELECT * FROM project WHERE id = :id")
    ProjectEntry loadProjectById(long id);

    @Query("SELECT * FROM project WHERE id = :id")
    LiveData<ProjectEntry> loadLiveDataProjectById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertProject(ProjectEntry projectEntry);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateProject(ProjectEntry projectEntry);

    @Delete
    void deleteProject(ProjectEntry projectEntry);

    @Query("DELETE FROM project")
    void deleteAllProjects();

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
    LiveData<List<Project>> loadProjectViews();

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
    LiveData<Project> loadProjectView(long id);

}
