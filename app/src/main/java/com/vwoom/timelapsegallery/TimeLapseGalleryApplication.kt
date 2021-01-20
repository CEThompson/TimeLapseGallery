package com.vwoom.timelapsegallery

import android.app.Application
import com.vwoom.timelapsegallery.di.app.AppComponent
import com.vwoom.timelapsegallery.di.app.AppModule
import com.vwoom.timelapsegallery.di.app.DaggerAppComponent
import timber.log.Timber

// TODO: (TLG rebrand) change to SERENDIPITY
// TODO: (TLG rebrand) create mermaid splash - investigate bombs and bees - animate water
// TODO: (TLG rebrand) add a map, bottom NAV, sidenav?

// TODO: (1.4) convert NWS Api to OpenWeather?
// TODO: (1.4) implement CI/CD

// TODO: (when scaling an issue) implement left handed and right handed modes
// TODO: (when scaling an issue) implement content grouping where appropriate for accessibility
// TODO: (when scaling an issue) consider any content descriptions that need dynamic content descriptions with live regions

// TODO: (deferred) consider adding quick input mode
// TODO: (deferred) consider modularizing app  [camera - gallery]
// TODO: (deferred) investigate night light mode? How do stargazing apps do it?

// TODO: (when stable) convert to data store
// TODO: (when stable) convert moshi to kotlinx serialization
// TODO: (when stable) convert all usage of java time to kotlinx datetime
// TODO: (when stable) convert to hilt?

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