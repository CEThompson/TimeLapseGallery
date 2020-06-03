package com.vwoom.timelapsegallery.di

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.dao.*
import com.vwoom.timelapsegallery.data.datasource.WeatherRemoteDataSource
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DataModule {
    /*@Provides
    fun provideSharedPreferences(
            app: Application
    ): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)*/

    @Provides
    @Singleton
    fun provideRemoteDataSource(): WeatherRemoteDataSource {
        return WeatherRemoteDataSource()
    }

    @Provides
    @Singleton
    fun provideDb(app: Application): TimeLapseDatabase {
        return TimeLapseDatabase.getInstance(app)
    }

    @Provides
    fun provideCoverPhotoDao(db: TimeLapseDatabase): CoverPhotoDao {
        return db.coverPhotoDao()
    }

    @Provides
    fun providePhotoDao(db: TimeLapseDatabase): PhotoDao {
        return db.photoDao()
    }

    @Provides
    fun provideProjectDao(db: TimeLapseDatabase): ProjectDao {
        return db.projectDao()
    }

    @Provides
    fun provideProjectScheduleDao(db: TimeLapseDatabase): ProjectScheduleDao {
        return db.projectScheduleDao()
    }

    @Provides
    fun provideProjectTagDao(db: TimeLapseDatabase): ProjectTagDao {
        return db.projectTagDao()
    }

    @Provides
    fun provideTagDao(db: TimeLapseDatabase): TagDao {
        return db.tagDao()
    }

    @Provides
    fun provideWeatherDao(db: TimeLapseDatabase): WeatherDao {
        return db.weatherDao()
    }

}
