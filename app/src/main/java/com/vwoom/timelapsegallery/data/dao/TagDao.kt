package com.vwoom.timelapsegallery.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vwoom.timelapsegallery.data.entry.TagEntry

@Dao
interface TagDao {
    @Query("SELECT * FROM tag")
    fun loadAllTags(): LiveData<List<TagEntry>>

    @Query("SELECT * FROM tag WHERE id = :id")
    fun loadTagById(id: Long): TagEntry

    @Insert
    fun insertTag(tagEntry: TagEntry)

    @Delete
    fun deleteTag(tagEntry: TagEntry)

    @Update
    fun updateTag(tagEntry: TagEntry)
}