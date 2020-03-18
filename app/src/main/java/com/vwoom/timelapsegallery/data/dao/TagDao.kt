package com.vwoom.timelapsegallery.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vwoom.timelapsegallery.data.entry.TagEntry

@Dao
interface TagDao {
    @Query("SELECT * FROM tag")
    fun getTags(): List<TagEntry>

    @Query("SELECT * FROM tag")
    fun getTagsLiveData(): LiveData<List<TagEntry>>

    @Query("SELECT * FROM tag WHERE id = :id")
    suspend fun getTagById(id: Long): TagEntry

    @Query("SELECT * FROM tag WHERE text = :text")
    suspend fun getTagByText(text: String): TagEntry?

    @Insert
    suspend fun insertTag(tagEntry: TagEntry): Long

    @Delete
    suspend fun deleteTag(tagEntry: TagEntry)

    @Update
    fun updateTag(tagEntry: TagEntry)

    @Query("DELETE FROM tag")
    suspend fun deleteAllTags()
}