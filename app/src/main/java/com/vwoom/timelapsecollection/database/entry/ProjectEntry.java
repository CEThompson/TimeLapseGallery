package com.vwoom.timelapsecollection.database.entry;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "project")
public class ProjectEntry implements Parcelable {

    @PrimaryKey(autoGenerate = true) private long id;
    private String name;
    private String thumbnail_url;
    private int schedule;
    private long schedule_next_submission;
    private long timestamp;

    /* For inserting with auto-generated ID */
    @Ignore
    public ProjectEntry(String name,
                        String thumbnail_url,
                        int schedule,
                        long schedule_next_submission,
                        long timestamp){
        this.name = name;
        this.thumbnail_url = thumbnail_url;
        this.schedule = schedule;
        this.schedule_next_submission = schedule_next_submission;
        this.timestamp = timestamp;
    }

    public ProjectEntry(long id,
                        String name,
                        String thumbnail_url,
                        int schedule,
                        long schedule_next_submission,
                        long timestamp){
        this.id = id;
        this.name = name;
        this.thumbnail_url = thumbnail_url;
        this.schedule= schedule;
        this.schedule_next_submission = schedule_next_submission;
        this.timestamp = timestamp;
    }

    /* Getters */
    public long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getThumbnail_url() { return thumbnail_url; }
    public int getSchedule() { return schedule; }
    public long getSchedule_next_submission() { return schedule_next_submission; }
    public long getTimestamp() { return timestamp; }

    /* Setters */
    public void setId(long id) { this.id = id; }
    public void setName(@NonNull String name) { this.name = name; }
    public void setThumbnail_url(String thumbnail_url) {
        this.thumbnail_url = thumbnail_url;
    }
    public void setSchedule(int schedule) {
        this.schedule = schedule;
    }
    public void setSchedule_next_submission(long schedule_next_submission) { this.schedule_next_submission = schedule_next_submission; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }


    /* Parcelable functionality */
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeString(name);
        parcel.writeString(thumbnail_url);
        parcel.writeInt(schedule);
        parcel.writeLong(schedule_next_submission);
        parcel.writeLong(timestamp);
    }

    public static final Parcelable.Creator<ProjectEntry> CREATOR
            = new Parcelable.Creator<ProjectEntry>() {
        public ProjectEntry createFromParcel(Parcel in) {
            return new ProjectEntry(in);
        }

        public ProjectEntry[] newArray(int size) {
            return new ProjectEntry[size];
        }
    };

    private ProjectEntry(Parcel in){
        id = in.readLong();
        name = in.readString();
        thumbnail_url = in.readString();
        schedule = in.readInt();
        schedule_next_submission = in.readLong();
        timestamp = in.readLong();
    }
}
