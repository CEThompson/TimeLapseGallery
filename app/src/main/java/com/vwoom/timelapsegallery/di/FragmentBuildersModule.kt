package com.vwoom.timelapsegallery.di

import com.vwoom.timelapsegallery.camera2.Camera2Fragment
import com.vwoom.timelapsegallery.detail.DetailFragment
import com.vwoom.timelapsegallery.gallery.GalleryFragment
import com.vwoom.timelapsegallery.settings.SettingsFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FragmentBuildersModule {
    @ContributesAndroidInjector
    abstract fun contributeGalleryFragment(): GalleryFragment

    @ContributesAndroidInjector
    abstract fun contributeDetailFragment(): DetailFragment

    @ContributesAndroidInjector
    abstract fun contributeSettingsFragment(): SettingsFragment

    @ContributesAndroidInjector
    abstract fun contributeCamera2Fragment(): Camera2Fragment
}