package com.vwoom.timelapsegallery.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.vwoom.timelapsegallery.database.entry.ProjectScheduleEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectTagEntry;

import java.util.List;

@Dao
public interface ProjectScheduleDao {

    @Query ("SELECT * FROM project_schedule WHERE project_id = :projectId")
    ProjectScheduleEntry loadScheduleByProjectId(long projectId);

    @Insert
    void insertProjectSchedule(ProjectScheduleEntry projectScheduleEntry);

    @Delete
    void deleteProjectSchedule(ProjectScheduleEntry projectScheduleEntry);

    @Update
    void updateProjectSchedule(ProjectScheduleEntry projectScheduleEntry);

}