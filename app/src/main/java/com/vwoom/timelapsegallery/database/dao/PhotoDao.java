package com.vwoom.timelapsegallery.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.vwoom.timelapsegallery.database.entry.PhotoEntry;

import java.util.List;

@Dao
public interface PhotoDao {

    @Query("SELECT * FROM photo WHERE project_id = :project_id ORDER BY timestamp")
    LiveData<List<PhotoEntry>> loadAllPhotosByProjectId(long project_id);

    @Query("SELECT * FROM photo WHERE project_id = :project_id ORDER BY timestamp")
    List<PhotoEntry> loadAllPhotosByProjectId_NonLiveData(long project_id);

    @Insert
    void insertPhoto(PhotoEntry photoEntry);

    @Update
    void updatePhoto(PhotoEntry photoEntry);

    @Delete
    void deletePhoto(PhotoEntry photoEntry);

    @Query("DELETE FROM photo")
    void deleteAllPhotos();
}
