package com.vwoom.timelapsegallery.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vwoom.timelapsegallery.database.entry.CoverPhotoEntry

@Dao
interface CoverPhotoDao {
    @Query("SELECT * FROM cover_photo WHERE project_id =:projectId")
    fun getCoverPhoto(projectId: Long): LiveData<CoverPhotoEntry?>?

    @Query("SELECT * FROM cover_photo WHERE project_id =:projectId")
    fun getCoverPhoto_nonLiveData(projectId: Long): CoverPhotoEntry?

    @get:Query("SELECT * FROM cover_photo")
    val allCoverPhotos: LiveData<List<CoverPhotoEntry?>?>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPhoto(entry: CoverPhotoEntry?)

    @Update
    fun updatePhoto(entry: CoverPhotoEntry?)

    @Delete
    fun deletePhoto(entry: CoverPhotoEntry?)
}