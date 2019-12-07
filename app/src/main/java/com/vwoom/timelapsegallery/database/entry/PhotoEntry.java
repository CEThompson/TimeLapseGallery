package com.vwoom.timelapsegallery.database.entry;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "photo")
public class PhotoEntry implements Parcelable {

    @PrimaryKey(autoGenerate = true) private long id;
    private long project_id;
    private long timestamp;

    /* For inserting with auto-generated ID */
    @Ignore
    public PhotoEntry(long project_id, long timestamp) {
        this.project_id = project_id;
        this.timestamp = timestamp;
    }

    public PhotoEntry(long id, long project_id, long timestamp){
        this.id = id;
        this.project_id = project_id;
        this.timestamp = timestamp;
    }

    /* Getters */

    public long getId() {
        return id;
    }
    public long getProject_id() {
        return project_id;
    }
    public long getTimestamp() {
        return timestamp;
    }

    /* Setters */

    public void setId(long id) {
        this.id = id;
    }
    public void setProject_id(long project_id) {
        this.project_id = project_id;
    }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    /* Parcelable functionality */
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeLong(project_id);
        parcel.writeLong(timestamp);
    }

    public static final Parcelable.Creator<PhotoEntry> CREATOR
            = new Parcelable.Creator<PhotoEntry>() {
        public PhotoEntry createFromParcel(Parcel in) {
            return new PhotoEntry(in);
        }

        public PhotoEntry[] newArray(int size) {
            return new PhotoEntry[size];
        }
    };

    private PhotoEntry(Parcel in){
        id = in.readLong();
        project_id = in.readLong();
        timestamp = in.readLong();
    }
}
