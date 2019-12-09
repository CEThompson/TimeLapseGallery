package com.vwoom.timelapsegallery.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

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
                        .addMigrations(MIGRATION_1_2)
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

    static final Migration MIGRATION_1_2 = new Migration(1,2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create new tables
            // (1) Cover Photo
            // columns: project_id, photo_id

            // Create the table
            database.execSQL("CREATE TABLE IF NOT EXISTS cover_photo " +
                    "(project_id INTEGER PRIMARY KEY NOT NULL, photo_id INTEGER NOT NULL," +
                    "CONSTRAINT fk_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE," +
                    "CONSTRAINT fk_photo FOREIGN KEY (photo_id) REFERENCES photo (id) ON DELETE CASCADE)");

            // (2) Project Schedule
            // columns: project_id, schedule_time, interval_days
            database.execSQL("CREATE TABLE IF NOT EXISTS project_schedule " +
                    "(project_id INTEGER PRIMARY KEY NOT NULL, schedule_time INTEGER, interval_days INTEGER," +
                    "CONSTRAINT fk_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE)");

            // Copy data into new tables
            // (1) Project
            // initial columns: id, name, thumbnail_url, schedule, schedule_next_submission, timestamp
            // altered columns: id, project_name, cover_set_by_user

            // This WOULD be the pattern!
            // Create new table
            //database.execSQL("CREATE TABLE project_new (id LONG, project_name TEXT, cover_set_by_user INT, PRIMARY KEY(id))");
            // Copy the data
            //database.execSQL("INSERT INTO project_new (id, project_name) SELECT id, name FROM project");
            // Remove old table
            //database.execSQL("DROP TABLE project");
            // Change table name to old one
            //database.execSQL("ALTER TABLE project_new RENAME TO project");

            database.execSQL("DROP TABLE project");
            database.execSQL("CREATE TABLE IF NOT EXISTS project " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, project_name TEXT, cover_set_by_user INTEGER NOT NULL)");

            // (2) Photo
            // init: id, project_id, url, timestamp
            // altered: id, project_id, timestamp

            // Create the new
            //database.execSQL("CREATE TABLE photo_new (id LONG, project_id LONG, timestamp LONG, PRIMARY KEY(id))");
            // Add the old
            //database.execSQL("DROP TABLE photo");
            // Rename the new
            //database.execSQL("ALTER TABLE photo_new RENAME TO photo");

            database.execSQL("DROP TABLE photo");
            database.execSQL("CREATE TABLE IF NOT EXISTS photo " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, project_id INTEGER NOT NULL, timestamp INTEGER NOT NULL)");

            // (3) Project Tag
            // init: id, tag_id, project_id
            // altered: project_id, tag_id
            database.execSQL("DROP TABLE project_tag");
            database.execSQL("CREATE TABLE IF NOT EXISTS project_tag " +
                    "(project_id INTEGER PRIMARY KEY NOT NULL, tag_id INTEGER NOT NULL, " +
                    "CONSTRAINT fk_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE," +
                    "CONSTRAINT fk_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE)");

            // (4) Tag
            // init: id, title
            // altered: project_id, tag
            database.execSQL("DROP TABLE tag");
            database.execSQL("CREATE TABLE IF NOT EXISTS tag " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tag TEXT)");

        }
    };

}
