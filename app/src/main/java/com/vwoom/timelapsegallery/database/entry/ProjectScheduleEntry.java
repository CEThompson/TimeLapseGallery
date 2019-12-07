package com.vwoom.timelapsegallery.database.entry;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity (tableName = "project_schedule")
public class ProjectScheduleEntry {

    @PrimaryKey private long project_id;
    private long schedule_time;
    private int interval_days;

    public ProjectScheduleEntry(long photo_id, long schedule_time, int interval_days){
        this.project_id = project_id;
        this.schedule_time = schedule_time;
        this.interval_days = interval_days;
    }

    public long getProjectId() {
        return project_id;
    }
    public long getScheduleTime() {
        return schedule_time;
    }
    public int getIntervalDays() {
        return interval_days;
    }

    public void setProjectId(long project_id) {
        this.project_id = project_id;
    }
    public void setScheduleTime(long schedule_time) {
        this.schedule_time = schedule_time;
    }
    public void setIntervalDays(int interval_days) {
        this.interval_days = interval_days;
    }
}
