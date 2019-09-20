package com.vwoom.timelapsegallery.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.vwoom.timelapsegallery.database.entry.ProjectTagEntry;

import java.util.List;

@Dao
public interface ProjectTagDao {

    @Query ("SELECT * FROM project_tag WHERE project_id = :projectId")
    List<ProjectTagEntry> loadTagsByProjectId(long projectId);

    @Insert
    void insertProjectTag(ProjectTagEntry projectTagEntry);

    @Delete
    void deleteProjectTag(ProjectTagEntry projectTagEntry);

    @Update
    void updateProjectTag(ProjectTagEntry projectTagEntry);

}
