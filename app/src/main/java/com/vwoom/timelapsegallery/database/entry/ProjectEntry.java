package com.vwoom.timelapsegallery.database.entry;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "project")
public class ProjectEntry implements Parcelable {

    @PrimaryKey(autoGenerate = true) private long project_id;
    private String project_name;
    private Long cover_photo_id;

    /* For inserting with auto-generated ID */
    @Ignore
    public ProjectEntry(@Nullable String project_name,
                        @Nullable Long cover_photo_id){
        this.project_name = project_name;
        this.cover_photo_id = cover_photo_id;
    }

    public ProjectEntry(long project_id,
                        @Nullable String project_name,
                        @Nullable Long cover_photo_id){
        this.project_id = project_id;
        this.project_name = project_name;
        this.cover_photo_id = cover_photo_id;
    }

    /* Getters */
    public long getId() {
        return project_id;
    }
    public String getName() {
        return project_name;
    }
    public Long getCoverPhotoId() { return cover_photo_id; }

    /* Setters */
    public void setId(long project_id) { this.project_id = project_id; }
    public void setName(@Nullable String project_name) { this.project_name = project_name; }
    public void setCoverPhotoId(@Nullable Long cover_photo_id) {
        this.cover_photo_id = cover_photo_id;
    }

    /* Parcelable functionality */
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(project_id);
        parcel.writeString(project_name);
        parcel.writeLong(cover_photo_id);
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
        project_id = in.readLong();
        project_name = in.readString();
        cover_photo_id = in.readLong();
    }
}
