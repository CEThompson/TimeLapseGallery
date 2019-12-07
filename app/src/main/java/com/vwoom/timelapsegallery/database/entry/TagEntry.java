package com.vwoom.timelapsegallery.database.entry;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "tag")
public class TagEntry {

    @PrimaryKey (autoGenerate = true) private long tag_id;
    private String tag;

    /* For inserting with auto-generated ID */
    @Ignore
    public TagEntry(String title){
        this.tag = tag;
    }

    public TagEntry(long tag_id, String tag){
        this.tag_id = tag_id;
        this.tag = tag;
    }

    public long getTagId() {
        return tag_id;
    }
    public String getTag() {
        return tag;
    }

    public void setTagId(long id) {
        this.tag_id = tag_id;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }
}
