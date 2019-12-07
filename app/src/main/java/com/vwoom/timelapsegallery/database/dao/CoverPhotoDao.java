package com.vwoom.timelapsegallery.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.vwoom.timelapsegallery.database.entry.CoverPhotoEntry;
import com.vwoom.timelapsegallery.database.entry.PhotoEntry;

import java.util.List;

@Dao
public interface CoverPhotoDao {
    @Query("SELECT * FROM cover_photo WHERE project_id =:projectId")
    LiveData<CoverPhotoEntry> getCoverPhoto(long projectId);

    @Query("SELECT * FROM cover_photo")
    LiveData<List<CoverPhotoEntry>> getAllCoverPhotos();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPhoto(PhotoEntry photoEntry);

    @Update
    void updatePhoto(PhotoEntry photoEntry);

    @Delete
    void deletePhoto(PhotoEntry photoEntry);
}
