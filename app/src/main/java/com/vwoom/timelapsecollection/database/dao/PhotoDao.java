package com.vwoom.timelapsecollection.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.vwoom.timelapsecollection.database.entry.PhotoEntry;

import java.util.List;

@Dao
public interface PhotoDao {

    @Query("SELECT * FROM photo WHERE project_id = :project_id ORDER BY timestamp")
    //@Query("SELECT * FROM photo WHERE project_id = :project_id")
    LiveData<List<PhotoEntry>> loadAllPhotosByProjectId(long project_id);


    @Query("SELECT * FROM photo WHERE project_id = :project_id ORDER BY timestamp")
        //@Query("SELECT * FROM photo WHERE project_id = :project_id")
    List<PhotoEntry> loadAllPhotosByProjectId_NonLiveData(long project_id);

    @Insert
    void insertPhoto(PhotoEntry photoEntry);

    @Update
    void updatePhoto(PhotoEntry photoEntry);

    @Delete
    void deletePhoto(PhotoEntry photoEntry);

}
