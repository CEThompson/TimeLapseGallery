package com.vwoom.timelapsegallery.database.entry;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "cover_photo",
        primaryKeys = {"project_id", "photo_id"},
        foreignKeys = {
                @ForeignKey(entity = ProjectEntry.class,
                        parentColumns = "id",
                        childColumns = "project_id",
                        onDelete = ForeignKey.CASCADE),

                @ForeignKey(entity = PhotoEntry.class,
                        parentColumns = "id",
                        childColumns = "photo_id",
                        onDelete = ForeignKey.CASCADE)
        })

public class CoverPhotoEntry {
    private long project_id;
    private long photo_id;

    public CoverPhotoEntry(long project_id, long photo_id) {
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
