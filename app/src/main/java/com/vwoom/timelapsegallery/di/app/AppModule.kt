package com.vwoom.timelapsegallery.di.app

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.dao.*
import com.vwoom.timelapsegallery.data.repository.*
import com.vwoom.timelapsegallery.data.source.IWeatherLocalDataSource
import com.vwoom.timelapsegallery.data.source.IWeatherRemoteDataSource
import com.vwoom.timelapsegallery.data.source.WeatherLocalDataSource
import com.vwoom.timelapsegallery.data.source.WeatherRemoteDataSource
import com.vwoom.timelapsegallery.di.ViewModelModule
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = [ViewModelModule::class])
object AppModule {

    // TODO: inject shared preferences
    @Provides
    fun provideSharedPreferences(
            app: Application
    ): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)

    @Provides
    @AppScope
    fun provideDb(app: Application): TimeLapseDatabase {
        return TimeLapseDatabase.getInstance(app)
    }

    @Provides
    @AppScope
    fun provideTagRepository(tagRepository: TagRepository): ITagRepository {
        return tagRepository
    }

    @Provides
    @AppScope
    fun provideWeatherRepository(weatherRepository: WeatherRepository): IWeatherRepository {
        return weatherRepository
    }

    @Provides
    @AppScope
    fun provideProjectRepository(projectRepository: ProjectRepository): IProjectRepository {
        return projectRepository
    }

    @Provides
    @AppScope
    fun provideRemoteDataSource(): IWeatherRemoteDataSource {
        return WeatherRemoteDataSource()
    }

    @Provides
    @AppScope
    fun provideLocalDataSource(weatherLocalDataSource: WeatherLocalDataSource): IWeatherLocalDataSource {
        return weatherLocalDataSource
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
