package com.vwoom.timelapsegallery.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vwoom.timelapsegallery.data.entry.CoverPhotoEntry

@Dao
interface CoverPhotoDao {
    @Query("SELECT * FROM cover_photo")
    fun getCoverPhotos(): List<CoverPhotoEntry>

    @Query("SELECT * FROM cover_photo WHERE project_id =:projectId")
    fun getCoverPhotoLiveData(projectId: Long): LiveData<CoverPhotoEntry>

    @Query("SELECT * FROM cover_photo WHERE project_id =:projectId")
    fun getCoverPhoto(projectId: Long): CoverPhotoEntry

    @Query("SELECT * FROM cover_photo")
    fun getAllCoverPhotosLiveData(): LiveData<List<CoverPhotoEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(entry: CoverPhotoEntry)

    @Update
    suspend fun updatePhoto(entry: CoverPhotoEntry)

    @Delete
    suspend fun deletePhoto(entry: CoverPhotoEntry)
}