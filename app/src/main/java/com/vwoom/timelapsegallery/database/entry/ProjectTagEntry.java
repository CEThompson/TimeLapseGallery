package com.vwoom.timelapsegallery.database.entry;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "project_tag")
public class ProjectTagEntry {

    @PrimaryKey private long id;
    private long tag_id;
    private long project_id;

    @Ignore
    public ProjectTagEntry(long tag_id, long project_id){
        this.tag_id = tag_id;
        this.project_id = project_id;
    }

    public ProjectTagEntry(long id, long tag_id, long project_id){
        this.id = id;
        this.tag_id = tag_id;
        this.project_id = project_id;
    }

    public long getId() {
        return id;
    }
    public long getTag_id() {
        return tag_id;
    }
    public long getProject_id() {
        return project_id;
    }

    public void setId(long id) {
        this.id = id;
    }
    public void setTag_id(long tag_id) {
        this.tag_id = tag_id;
    }
    public void setProject_id(long project_id) {
        this.project_id = project_id;
    }
}
