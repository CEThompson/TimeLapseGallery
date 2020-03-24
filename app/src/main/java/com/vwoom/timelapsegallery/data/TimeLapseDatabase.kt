package com.vwoom.timelapsegallery.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vwoom.timelapsegallery.data.dao.*
import com.vwoom.timelapsegallery.data.entry.*

@Database(entities = [ProjectEntry::class, PhotoEntry::class, TagEntry::class, ProjectTagEntry::class, ProjectScheduleEntry::class, CoverPhotoEntry::class], version = 2, exportSchema = true)
abstract class TimeLapseDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun photoDao(): PhotoDao
    abstract fun tagDao(): TagDao
    abstract fun projectTagDao(): ProjectTagDao
    abstract fun coverPhotoDao(): CoverPhotoDao
    abstract fun projectScheduleDao(): ProjectScheduleDao

    companion object {

        @Volatile private var instance: TimeLapseDatabase? = null
        private const val DATABASE_NAME = "time_lapse_db"

        fun getInstance(context: Context): TimeLapseDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also {instance = it}
            }
        }

        private fun buildDatabase(context: Context): TimeLapseDatabase {
            return Room.databaseBuilder(context, TimeLapseDatabase::class.java, DATABASE_NAME)
                    .addMigrations(MIGRATION_1_2)
                    .build()
        }

        // TODO test migration
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // TODO Create new tables
                // (1) Cover Photo
                // columns: project_id, photo_id
                database.execSQL("CREATE TABLE IF NOT EXISTS cover_photo " +
                        "(project_id INTEGER PRIMARY KEY NOT NULL, photo_id INTEGER NOT NULL," +
                        "CONSTRAINT fk_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE," +
                        "CONSTRAINT fk_photo FOREIGN KEY (photo_id) REFERENCES photo (id) ON DELETE CASCADE)")

                // (2) Project Schedule
                // columns: project_id, interval_days
                database.execSQL("CREATE TABLE IF NOT EXISTS project_schedule " +
                        "(project_id INTEGER PRIMARY KEY NOT NULL, " +
                        "interval_days INTEGER," +
                        "CONSTRAINT fk_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE)")

                // Copy data into new tables
                // (3) Project
                // initial columns: id, name, thumbnail_url, schedule, schedule_next_submission, timestamp
                // altered columns: id, project_name

                // This WOULD be the pattern!
                // Create new table
                database.execSQL("CREATE TABLE project_new " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "project_name TEXT)")
                // Copy the data
                database.execSQL("INSERT INTO project_new " +
                        "(id, project_name) SELECT id, name FROM project")
                // Remove old table
                database.execSQL("DROP TABLE project");
                // Change table name to old one
                database.execSQL("ALTER TABLE project_new RENAME TO project")

                /* This block could be used to drop the old data without migrating
                database.execSQL("DROP TABLE project")
                database.execSQL("CREATE TABLE IF NOT EXISTS project " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "project_name TEXT)")
                */

                // (4) Photo
                // init: id, project_id, url, timestamp
                // altered: id, project_id, timestamp
                // Create the new photo table
                database.execSQL("CREATE TABLE photo_new " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NO NULL, " +
                        "project_id INTEGER NOT NULL, " +
                        "timestamp INTEGER NOT NULL)");
                // Copy the data
                database.execSQL("INSERT INTO photo_new " +
                        "(id, project_id, timestamp) SELECT id, project_id, timestamp FROM photo")
                // Delete the old table
                database.execSQL("DROP TABLE photo");
                // Rename the new
                database.execSQL("ALTER TABLE photo_new RENAME TO photo");

                /* This block could be used to simply drop the old table and create the new without migrating data
                database.execSQL("DROP TABLE photo")
                database.execSQL("CREATE TABLE IF NOT EXISTS photo " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "project_id INTEGER NOT NULL, " +
                        "timestamp INTEGER NOT NULL)")
                */

                /*
                * Note these two tables do not need data migration
                 */
                // (5) Project Tag
                // init: id, tag_id, project_id
                // altered: project_id, tag_id
                database.execSQL("DROP TABLE project_tag")
                database.execSQL("CREATE TABLE IF NOT EXISTS project_tag " +
                        "(project_id INTEGER PRIMARY KEY NOT NULL, " +
                        "tag_id INTEGER NOT NULL, " +
                        "CONSTRAINT fk_project FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE," +
                        "CONSTRAINT fk_tag FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE)")

                // (6) Tag
                // init: id, title
                // altered: project_id, tag
                database.execSQL("DROP TABLE tag")
                database.execSQL("CREATE TABLE IF NOT EXISTS tag " +
                        "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tag TEXT NOT NULL)")
            }
        }
    }
}