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
    public TagEntry(String title){
        this.tag = tag;
    }

    public TagEntry(long tag_id, String tag){
        this.id = tag_id;
        this.tag = tag;
    }

    public long getTagId() {
        return id;
    }
    public String getTag() {
        return tag;
    }

    public void setTagId(long id) {
        this.id = this.id;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }
}
