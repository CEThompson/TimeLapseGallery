package com.vwoom.timelapsegallery.database.entry;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "tag")
public class TagEntry {

    @PrimaryKey (autoGenerate = true) private long id;
    private String tag;

    /* For inserting with auto-generated ID */
    @Ignore
    public TagEntry(String tag){
        this.tag = tag;
    }

    public TagEntry(long id, String tag){
        this.id = id;
        this.tag = tag;
    }

    public long getId() {
        return id;
    }
    public String getTag() {
        return tag;
    }

    public void setId(long id) {
        this.id = id;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }
}
