package com.vwoom.timelapsegallery

import android.app.Application
import com.vwoom.timelapsegallery.di.app.AppComponent
import com.vwoom.timelapsegallery.di.app.AppModule
import com.vwoom.timelapsegallery.di.app.DaggerAppComponent
import timber.log.Timber

// TODO: TLG 1.4
// TODO: use app bundling for 1.4 release
// TODO: fix proguard minification and obfuscation
// TODO: change to SERENDIPITY
// TODO: create mermaid splash - investigate bombs and bees - animate water
// TODO: add a map, bottom NAV, sidenav?
// TODO: convert to data store
// TODO: convert moshi to kotlinx serialization
// TODO: convert all usage of java time to kotlinx datetime
// TODO: modularize app? - camera - gallery
// TODO: try out trunk based development OR standard gitflow
// TODO: implement CI/CD
// TODO: convert to hilt
// TODO: re-brand to Time Lapse Garden
// TODO: investigate night light mode? How do stargazing apps do it?


// TODO add quick project add input mode
// TODO convert NWS Api to OpenWeather?


class TimeLapseGalleryApplication : Application() {

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.builder()
                .appModule(AppModule(this))
                .build()
    }
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())
    }
}