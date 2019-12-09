package com.vwoom.timelapsegallery.database.entry;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "project_schedule",
        primaryKeys = "project_id",
        foreignKeys = {
                @ForeignKey(entity = ProjectEntry.class,
                        parentColumns = "id",
                        childColumns = "project_id",
                        onDelete = ForeignKey.CASCADE),
        })

public class ProjectScheduleEntry {
    private long project_id;
    private Long schedule_time;
    private Integer interval_days;

    public ProjectScheduleEntry(long project_id, @Nullable Long schedule_time, @Nullable Integer interval_days){
        this.project_id = project_id;
        this.schedule_time = schedule_time;
        this.interval_days = interval_days;
    }

    public long getProject_id() {
        return project_id;
    }
    public Long getSchedule_time() {
        return schedule_time;
    }
    public Integer getInterval_days() {
        return interval_days;
    }
    public void setProject_id(long project_id) {
        this.project_id = project_id;
    }
    public void setSchedule_time(@Nullable  Long schedule_time) {
        this.schedule_time = schedule_time;
    }
    public void setInterval_days(@Nullable Integer interval_days) {
        this.interval_days = interval_days;
    }
}
