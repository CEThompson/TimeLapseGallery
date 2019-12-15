package com.vwoom.timelapsegallery.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vwoom.timelapsegallery.data.entry.PhotoEntry

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photo WHERE project_id = :project_id ORDER BY timestamp")
    fun loadAllPhotosByProjectId(project_id: Long): LiveData<List<PhotoEntry?>?>?

    @Query("SELECT * FROM photo WHERE project_id = :project_id ORDER BY timestamp")
    fun loadAllPhotosByProjectId_NonLiveData(project_id: Long): List<PhotoEntry?>?

    @Query("SELECT * FROM photo WHERE project_id = :project_id AND id = :photo_id")
    fun loadPhoto(project_id: Long, photo_id: Long): PhotoEntry?

    @Insert
    fun insertPhoto(photoEntry: PhotoEntry?): Long

    @Update
    fun updatePhoto(photoEntry: PhotoEntry?)

    @Delete
    fun deletePhoto(photoEntry: PhotoEntry?)

    @Query("DELETE FROM photo")
    fun deleteAllPhotos()
}