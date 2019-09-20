package com.vwoom.timelapsegallery.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.vwoom.timelapsegallery.database.entry.TagEntry;

import java.util.List;

@Dao
public interface TagDao {

    @Query("SELECT * FROM tag")
    List<TagEntry> loadAllTags();

    @Insert
    void insertTag(TagEntry tagEntry);

    @Delete
    void deleteTag(TagEntry tagEntry);

    @Update
    void updateTag(TagEntry tagEntry);

}
