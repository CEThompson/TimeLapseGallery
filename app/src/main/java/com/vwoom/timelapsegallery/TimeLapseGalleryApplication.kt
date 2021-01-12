package com.vwoom.timelapsegallery

import android.app.Application
import com.vwoom.timelapsegallery.di.app.AppInjector
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import timber.log.Timber
import javax.inject.Inject

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

// TODO add mini fab to scroll down to bottom in the gallery

class TimeLapseGalleryApplication : Application(), HasAndroidInjector {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onCreate() {
        super.onCreate()
        AppInjector.init(this)
        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())
    }
}