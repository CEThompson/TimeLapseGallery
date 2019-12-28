package com.vwoom.timelapsegallery.data.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*
import com.vwoom.timelapsegallery.data.entry.PhotoEntry

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photo WHERE project_id = :project_id ORDER BY timestamp")
    fun loadAllPhotosByProjectId(project_id: Long): LiveData<List<PhotoEntry>>

    @Query("SELECT * FROM photo WHERE project_id = :project_id ORDER BY timestamp")
    fun loadAllPhotosByProjectId_NonLiveData(project_id: Long): List<PhotoEntry>

    @Query("SELECT * FROM photo WHERE project_id = :project_id AND id = :photo_id")
    fun loadPhoto(project_id: Long, photo_id: Long): PhotoEntry

    @Query("SELECT * FROM photo WHERE project_id = :project_id ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPhoto(project_id: Long): PhotoEntry

    @Insert
    suspend fun insertPhoto(photoEntry: PhotoEntry): Long

    @Update
    suspend fun updatePhoto(photoEntry: PhotoEntry)

    @Delete
    suspend fun deletePhoto(photoEntry: PhotoEntry)

    @Query("DELETE FROM photo")
    suspend fun deleteAllPhotos()
}