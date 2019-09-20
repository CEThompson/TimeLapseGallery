package com.vwoom.timelapsecollection.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.vwoom.timelapsecollection.database.dao.PhotoDao;
import com.vwoom.timelapsecollection.database.dao.ProjectDao;
import com.vwoom.timelapsecollection.database.dao.ProjectTagDao;
import com.vwoom.timelapsecollection.database.dao.TagDao;
import com.vwoom.timelapsecollection.database.entry.PhotoEntry;
import com.vwoom.timelapsecollection.database.entry.ProjectEntry;
import com.vwoom.timelapsecollection.database.entry.ProjectTagEntry;
import com.vwoom.timelapsecollection.database.entry.TagEntry;

@Database(entities = {ProjectEntry.class, PhotoEntry.class, TagEntry.class, ProjectTagEntry.class}, version = 1, exportSchema = true)
public abstract class TimeLapseDatabase extends RoomDatabase{

    private static final Object LOCK = new Object();

    private static final String DATABASE_NAME = "time_lapse_db";
    private static TimeLapseDatabase sInstance;

    public static TimeLapseDatabase getInstance(Context context){
        if (sInstance == null){
            synchronized (LOCK){
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

}
