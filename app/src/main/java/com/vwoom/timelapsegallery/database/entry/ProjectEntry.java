package com.vwoom.timelapsegallery.database.entry;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "project")
public class ProjectEntry implements Parcelable {

    @PrimaryKey(autoGenerate = true) private long id;
    private String project_name;
    private boolean project_cover_set_by_user = false;

    /* For inserting with auto-generated ID */
    @Ignore
    public ProjectEntry(@Nullable String project_name,
                        boolean project_cover_set_by_user){
        this.project_name = project_name;
        this.project_cover_set_by_user = project_cover_set_by_user;
    }

    public ProjectEntry(long id,
                        @Nullable String project_name,
                        boolean project_cover_set_by_user){
        this.id = id;
        this.project_name = project_name;
        this.project_cover_set_by_user = project_cover_set_by_user;
    }

    /* Getters */
    public long getId() {
        return id;
    }
    public String getName() {
        return project_name;
    }
    public boolean isProject_cover_set_by_user() {
        return project_cover_set_by_user;
    }

    /* Setters */
    public void setId(long id) { this.id = id; }
    public void setName(@Nullable String project_name) { this.project_name = project_name; }

    public void setProject_cover_set_by_user(boolean project_cover_set_by_user) {
        this.project_cover_set_by_user = project_cover_set_by_user;
    }

    /* Parcelable functionality */
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeString(project_name);
        // Convert boolean to 0 = false, 1 = true;
        parcel.writeByte((byte) (project_cover_set_by_user ? 1 : 0));
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
        project_name = in.readString();
        // Convert 0 to false and 1 to true
        project_cover_set_by_user = in.readByte() != 0;
    }
}
