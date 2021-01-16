package com.vwoom.timelapsegallery.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    // Major restructure of database schema (implementing designs from databases for mere mortals)
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Step (1) Create new tables
            // Table 1: Cover Photo
            // columns: project_id, photo_id
            database.execSQL("CREATE TABLE IF NOT EXISTS cover_photo " +
                    "(project_id INTEGER NOT NULL, " +
                    "photo_id INTEGER NOT NULL, " +
                    "PRIMARY KEY(project_id), " +
                    "FOREIGN KEY(project_id) REFERENCES project(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(photo_id) REFERENCES photo(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            // Create the indexing for the photo table
            database.execSQL("CREATE INDEX IF NOT EXISTS index_cover_photo_project_id ON cover_photo(project_id)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_cover_photo_photo_id ON cover_photo(photo_id)")

            // Table 2: Project Schedule
            // columns: project_id, interval_days
            database.execSQL("CREATE TABLE IF NOT EXISTS project_schedule " +
                    "(project_id INTEGER NOT NULL, " +
                    "interval_days INTEGER NOT NULL, " +
                    "PRIMARY KEY(project_id), " +
                    "FOREIGN KEY(project_id) REFERENCES project(id) ON UPDATE NO ACTION ON DELETE CASCADE)")

            // Step (2) Copy data from old tables into new tables
            // Table 3: Project
            // initial columns: id, name, thumbnail_url, schedule, schedule_next_submission, timestamp
            // altered columns: id, project_name
            // Create the new table
            database.execSQL("CREATE TABLE project_new " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "project_name TEXT)")
            // Copy the data into the new table from the old table
            database.execSQL("INSERT INTO project_new " +
                    "(id, project_name) SELECT id, name FROM project")
            // Remove old table
            database.execSQL("DROP TABLE project")
            // Change new table name to old table name
            database.execSQL("ALTER TABLE project_new RENAME TO project")

            // Table 4: Photo
            // initial columns: id, project_id, url, timestamp
            // altered columns: id, project_id, timestamp
            // Create the new photo table
            database.execSQL("CREATE TABLE photo_new " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "project_id INTEGER NOT NULL, " +
                    "timestamp INTEGER NOT NULL, " +
                    "FOREIGN KEY(project_id) REFERENCES project(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            // Copy the data
            database.execSQL("INSERT INTO photo_new " +
                    "(id, project_id, timestamp) SELECT id, project_id, timestamp FROM photo")
            // Delete the old table
            database.execSQL("DROP TABLE photo")
            // Rename the new
            database.execSQL("ALTER TABLE photo_new RENAME TO photo")
            // Create the indexing for the photo table
            database.execSQL("CREATE INDEX IF NOT EXISTS index_photo_project_id ON photo(project_id)")

            /*
            * Note these two tables do not need data migration
             */
            // Step (3) recreate tag tables, were not used in TLG 1.0 therefore should contain no data
            // Table 5: Project Tag
            // init: id, tag_id, project_id
            // altered: id, project_id, tag_id
            database.execSQL("DROP TABLE IF EXISTS project_tag")
            database.execSQL("CREATE TABLE IF NOT EXISTS project_tag " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "project_id INTEGER NOT NULL, " +
                    "tag_id INTEGER NOT NULL, " +
                    "FOREIGN KEY(project_id) REFERENCES project(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(tag_id) REFERENCES tag(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_project_tag_project_id ON project_tag(project_id)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_project_tag_tag_id ON project_tag(tag_id)")

            // Table(6): Tag
            // init: id, title
            // altered: project_id, text
            database.execSQL("DROP TABLE IF EXISTS tag")
            database.execSQL("CREATE TABLE IF NOT EXISTS tag " +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "text TEXT NOT NULL)")

            // Create the project view
            database.execSQL("CREATE VIEW `project_view` AS " +
                    "SELECT " +
                    "project.id AS project_id, " +
                    "project.project_name AS project_name, " +
                    "project_schedule.interval_days AS interval_days, " +
                    "cover_photo.photo_id AS cover_photo_id, " +
                    "photo.timestamp AS cover_photo_timestamp " +
                    "FROM project " +
                    "LEFT JOIN project_schedule " +
                    "ON project.id = project_schedule.project_id " +
                    "LEFT JOIN cover_photo " +
                    "ON project.id = cover_photo.project_id " +
                    "LEFT JOIN photo " +
                    "ON cover_photo.photo_id = photo.id")
        }
    }

    // Creates a table to cache weather api JSON response as a string
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS weather " +
                    "(id INTEGER NOT NULL, " +
                    "forecastJsonString TEXT NOT NULL," +
                    "timestamp INTEGER NOT NULL, " +
                    "PRIMARY KEY(id)" +
                    ")")
        }
    }

    // Adds property to project to record when a project has been updated with a new photo
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE project " +
                    "ADD project_updated INTEGER NOT NULL DEFAULT 1")
        }
    }

    // Adds sensor data fields to photos
    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE photo " +
                    "ADD light TEXT")
            database.execSQL("ALTER TABLE photo " +
                    "ADD pressure TEXT")
            database.execSQL("ALTER TABLE photo " +
                    "ADD temp TEXT")
            database.execSQL("ALTER TABLE photo " +
                    "ADD humidity TEXT")
        }
    }
}