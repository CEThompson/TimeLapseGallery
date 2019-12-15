package com.vwoom.timelapsegallery.data.dao

import androidx.room.*
import com.vwoom.timelapsegallery.data.entry.TagEntry

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