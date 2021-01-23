package com.vwoom.timelapsegallery.di.app

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides

@Module
class AppModule(val application: Application) {

    @Provides
    fun application() = application

    @Provides
    @AppScope
    fun provideSharedPreferences(
            app: Application
    ): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)

}
