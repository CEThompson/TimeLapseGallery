package com.vwoom.timelapsegallery.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.vwoom.timelapsegallery.database.dao.CoverPhotoDao;
import com.vwoom.timelapsegallery.database.dao.PhotoDao;
import com.vwoom.timelapsegallery.database.dao.ProjectDao;
import com.vwoom.timelapsegallery.database.dao.ProjectScheduleDao;
import com.vwoom.timelapsegallery.database.dao.ProjectTagDao;
import com.vwoom.timelapsegallery.database.dao.TagDao;
import com.vwoom.timelapsegallery.database.entry.CoverPhotoEntry;
import com.vwoom.timelapsegallery.database.entry.PhotoEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectScheduleEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectTagEntry;
import com.vwoom.timelapsegallery.database.entry.TagEntry;

@Database(entities = {ProjectEntry.class,
                        PhotoEntry.class,
                        TagEntry.class,
                        ProjectTagEntry.class,
                        ProjectScheduleEntry.class,
                        CoverPhotoEntry.class},
            version = 2,
            exportSchema = true)
public abstract class TimeLapseDatabase extends RoomDatabase {

    private static final Object LOCK = new Object();

    private static final String DATABASE_NAME = "time_lapse_db";
    private static TimeLapseDatabase sInstance;

    public static TimeLapseDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (LOCK) {
                sInstance = Room.databaseBuilder(context.getApplicationContext(),
                        TimeLapseDatabase.class, TimeLapseDatabase.DATABASE_NAME)
                        .build();
            }
        }
        return sInstance;
    }

    public abstract ProjectDao projectDao();

    public abstract PhotoDao photoDao();

    public abstract TagDao tagDao();

    public abstract ProjectTagDao projectTagDao();

    public abstract CoverPhotoDao coverPhotoDao();

    public abstract ProjectScheduleDao projectScheduleDao();

}
