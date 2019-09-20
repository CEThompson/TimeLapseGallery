package com.vwoom.timelapsegallery.database.entry;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "tag")
public class TagEntry {

    @PrimaryKey (autoGenerate = true) private long id;
    private String title;

    /* For inserting with auto-generated ID */
    @Ignore
    public TagEntry(String title){
        this.title = title;
    }

    public TagEntry(long id, String title){
        this.id = id;
        this.title = title;
    }

    public long getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }

    public void setId(long id) {
        this.id = id;
    }
    public void setTitle(String title) {
        this.title = title;
    }
}
