package com.vwoom.timelapsegallery.database.entry;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

// TODO verify foreign keys for validation table
@Entity (tableName = "project_tag",
            primaryKeys = {"project_id", "tag_id"},
            foreignKeys = {
                @ForeignKey(entity = ProjectEntry.class,
                    parentColumns = "project_id", childColumns = "project_id",
                    onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = TagEntry.class,
                        parentColumns = "tag_id", childColumns = "tag_id",
                        onDelete = ForeignKey.CASCADE)
        })

public class ProjectTagEntry {
    private long project_id;
    private long tag_id;

    public ProjectTagEntry(long project_id, long tag_id){
        this.project_id = project_id;
        this.tag_id = tag_id;
    }

    public long getTagId() {
        return tag_id;
    }
    public long getProjectId() {
        return project_id;
    }

    public void setTagId(long tag_id) {
        this.tag_id = tag_id;
    }
    public void setProjectId(long project_id) {
        this.project_id = project_id;
    }
}
