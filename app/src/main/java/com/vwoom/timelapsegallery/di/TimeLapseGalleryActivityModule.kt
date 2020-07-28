package com.vwoom.timelapsegallery.di

import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class TimeLapseGalleryActivityModule {
    @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    abstract fun contributeTimeLapseGalleryActivity(): TimeLapseGalleryActivity
}
