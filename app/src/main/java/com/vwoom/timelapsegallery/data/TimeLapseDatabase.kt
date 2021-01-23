@file:Suppress("MemberVisibilityCanBePrivate")

package com.vwoom.timelapsegallery.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vwoom.timelapsegallery.data.Migrations.MIGRATION_1_2
import com.vwoom.timelapsegallery.data.Migrations.MIGRATION_2_3
import com.vwoom.timelapsegallery.data.Migrations.MIGRATION_3_4
import com.vwoom.timelapsegallery.data.Migrations.MIGRATION_4_5
import com.vwoom.timelapsegallery.data.dao.*
import com.vwoom.timelapsegallery.data.entry.*
import com.vwoom.timelapsegallery.data.view.ProjectView

@Database(entities = [
    ProjectEntry::class,
    PhotoEntry::class,
    TagEntry::class,
    ProjectTagEntry::class,
    ProjectScheduleEntry::class,
    CoverPhotoEntry::class,
    WeatherEntry::class],
        views = [ProjectView::class],
        version = 5,
        exportSchema = true)
abstract class TimeLapseDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun photoDao(): PhotoDao
    abstract fun tagDao(): TagDao
    abstract fun projectTagDao(): ProjectTagDao
    abstract fun coverPhotoDao(): CoverPhotoDao
    abstract fun projectScheduleDao(): ProjectScheduleDao
    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var instance: TimeLapseDatabase? = null
        const val DATABASE_NAME = "time_lapse_db"

        fun getInstance(context: Context): TimeLapseDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): TimeLapseDatabase {
            return Room.databaseBuilder(context, TimeLapseDatabase::class.java, DATABASE_NAME)
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .build()
        }

    }
}