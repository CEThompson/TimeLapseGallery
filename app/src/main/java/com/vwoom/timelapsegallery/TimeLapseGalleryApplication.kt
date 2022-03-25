package com.vwoom.timelapsegallery

import android.app.Application
import com.vwoom.timelapsegallery.di.app.AppComponent
import com.vwoom.timelapsegallery.di.app.AppModule
import com.vwoom.timelapsegallery.di.app.DaggerAppComponent
import timber.log.Timber

// TODO: (1.4) Gather feedback for 1.4 update

// TODO: Consider rebranding
// TODO: (TLG rebrand) change to SERENDIPITY
// TODO: (TLG rebrand) create mermaid splash - investigate bombs and bees - animate water
// TODO: (TLG rebrand) add a map, bottom NAV, sidenav?

// TODO: (1.4) convert NWS Api to OpenWeather?
// TODO: (1.4) implement CI/CD

// Consider implementing left handed and right handed modes
// Consider implement content grouping where appropriate for accessibility
// Consider consider any content descriptions that need dynamic content descriptions with live regions

// TODO: (deferred) consider adding quick input mode
// TODO: (deferred) consider modularizing app  [camera - gallery]
// TODO: (deferred) investigate night light mode? How do stargazing apps do it?

// TODO: (when stable) consider converting to data store
// TODO: (when stable) consider converting moshi to kotlinx serialization
// TODO: (when stable) consider converting all usage of java time to kotlinx datetime
// TODO: (when stable) consider converting to hilt

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