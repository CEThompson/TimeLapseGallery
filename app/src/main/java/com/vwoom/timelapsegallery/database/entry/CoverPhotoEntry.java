package com.vwoom.timelapsegallery.database.entry;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity (tableName = "cover_photo")
public class CoverPhotoEntry {

    @PrimaryKey long project_id;
    long photo_id;

    public CoverPhotoEntry(long project_id, long photo_id){
        this.project_id = project_id;
        this.photo_id = photo_id;
    }

    // Setters
    public void setProject_id(long project_id) {
        this.project_id = project_id;
    }
    public void setPhoto_id(long photo_id) {
        this.photo_id = photo_id;
    }

    // Getters
    public long getProject_id() {
        return project_id;
    }
    public long getPhoto_id() {
        return photo_id;
    }
}
