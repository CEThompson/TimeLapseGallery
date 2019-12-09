package com.vwoom.timelapsegallery.database.entry;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "project")
public class ProjectEntry {

    @PrimaryKey(autoGenerate = true) private long id;
    private String project_name;
    private int cover_set_by_user = 0;

    /* For inserting with auto-generated ID */
    @Ignore
    public ProjectEntry(@Nullable String project_name,
                        int cover_set_by_user){
        this.project_name = project_name;
        this.cover_set_by_user = cover_set_by_user;
    }

    public ProjectEntry(long id,
                        @Nullable String project_name,
                        int cover_set_by_user){
        this.id = id;
        this.project_name = project_name;
        this.cover_set_by_user = cover_set_by_user;
    }

    /* Getters */

    public long getId() {
        return id;
    }
    public String getProject_name() {
        return project_name;
    }
    public int getCover_set_by_user() {
        return cover_set_by_user;
    }

    /* Setters */
    public void setId(long id) { this.id = id; }
    public void setProject_name(@Nullable String project_name) {
        this.project_name = project_name;
    }

    public void setCover_set_by_user(int cover_set_by_user) {
        this.cover_set_by_user = cover_set_by_user;
    }
}
