package com.vwoom.timelapsegallery.di

import android.app.Application
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.dao.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = [ViewModelModule::class])
class AppModule {
    @Singleton
    @Provides
    fun provideDb(app: Application): TimeLapseDatabase {
        return TimeLapseDatabase.getInstance(app)
    }

    @Singleton
    @Provides
    fun provideCoverPhotoDao(db: TimeLapseDatabase): CoverPhotoDao {
        return db.coverPhotoDao()
    }

    @Singleton
    @Provides
    fun providePhotoDao(db: TimeLapseDatabase): PhotoDao {
        return db.photoDao()
    }

    @Singleton
    @Provides
    fun provideProjectDao(db: TimeLapseDatabase): ProjectDao {
        return db.projectDao()
    }

    @Singleton
    @Provides
    fun provideProjectScheduleDao(db: TimeLapseDatabase): ProjectScheduleDao {
        return db.projectScheduleDao()
    }

    @Singleton
    @Provides
    fun provideProjectTag(db: TimeLapseDatabase): ProjectTagDao {
        return db.projectTagDao()
    }

    @Singleton
    @Provides
    fun provideTagDao(db: TimeLapseDatabase): TagDao {
        return db.tagDao()
    }

}