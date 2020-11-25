package com.vwoom.timelapsegallery

import android.app.Application
import com.vwoom.timelapsegallery.di.AppInjector
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import timber.log.Timber
import javax.inject.Inject

// TODO: TLG 1.4
// TODO: change to SERENDIPITY
// TODO: create mermaid splash - investigate bombs and bees - animate water
// TODO: add a map, bottom NAV, sidenav?
// TODO: convert all databinding to viewbinding
// TODO: convert moshi to kotlinx serialization
// TODO: convert all usage of java time to kotlinx datetime
// TODO: modularize app? - camera - gallery
// TODO: try out trunk based development OR standard gitflow
// TODO: implement CI/CD
// TODO: convert to hilt
// TODO: re-brand to Time Lapse Garden
// TODO: bind all available sensor data to camera and save sensor data (as exif?)
// TODO: add GPS data to photos as setting
// TODO: measure & display ambient light
// TODO: measure & display ambient pressure
// TODO: measure & display ambient temperature (and device temperature)
// TODO: measure & display relative humidity
// TODO: calc dew point if possible
// TODO: investigate night light mode? How do stargazing apps do it?

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