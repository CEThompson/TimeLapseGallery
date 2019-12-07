package com.vwoom.timelapsegallery.database.entry;

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
    private long schedule_time;
    private int interval_days;

    public ProjectScheduleEntry(long project_id, long schedule_time, int interval_days){
        this.project_id = project_id;
        this.schedule_time = schedule_time;
        this.interval_days = interval_days;
    }

    public long getProject_id() {
        return project_id;
    }
    public long getSchedule_time() {
        return schedule_time;
    }
    public int getInterval_days() {
        return interval_days;
    }
    public void setProject_id(long project_id) {
        this.project_id = project_id;
    }
    public void setSchedule_time(long schedule_time) {
        this.schedule_time = schedule_time;
    }
    public void setInterval_days(int interval_days) {
        this.interval_days = interval_days;
    }
}
