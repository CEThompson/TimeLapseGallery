package com.vwoom.timelapsegallery.database.dao

import androidx.room.*
import com.vwoom.timelapsegallery.database.entry.TagEntry

@Dao
interface TagDao {
    @Query("SELECT * FROM tag")
    fun loadAllTags(): List<TagEntry?>?

    @Insert
    fun insertTag(tagEntry: TagEntry?)

    @Delete
    fun deleteTag(tagEntry: TagEntry?)

    @Update
    fun updateTag(tagEntry: TagEntry?)
}