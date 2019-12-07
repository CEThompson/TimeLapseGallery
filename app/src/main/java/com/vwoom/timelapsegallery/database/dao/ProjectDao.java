package com.vwoom.timelapsegallery.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.vwoom.timelapsegallery.database.entry.ProjectEntry;

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
}
