package com.vwoom.timelapsegallery.database.entry;

import androidx.room.Entity;
import androidx.room.ForeignKey;

// TODO verify cascade deletion works appropriately
@Entity (tableName = "project_tag",
            primaryKeys = {"project_id"},
            foreignKeys = {
                @ForeignKey(entity = ProjectEntry.class,
                        parentColumns = "id", childColumns = "project_id",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = TagEntry.class,
                        parentColumns = "id", childColumns = "tag_id",
                        onDelete = ForeignKey.CASCADE)
        })
// TODO set index for columns

public class ProjectTagEntry {
    private long project_id;
    private long tag_id;

    public ProjectTagEntry(long project_id, long tag_id){
        this.project_id = project_id;
        this.tag_id = tag_id;
    }

    public long getProject_id() {
        return project_id;
    }
    public long getTag_id() {
        return tag_id;
    }

    public void setProject_id(long project_id) {
        this.project_id = project_id;
    }
    public void setTag_id(long tag_id) {
        this.tag_id = tag_id;
    }
}
