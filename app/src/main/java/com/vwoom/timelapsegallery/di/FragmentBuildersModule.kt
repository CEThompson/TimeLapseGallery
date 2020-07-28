package com.vwoom.timelapsegallery.di

import com.vwoom.timelapsegallery.detail.DetailFragment
import com.vwoom.timelapsegallery.gallery.GalleryFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FragmentBuildersModule {
    @ContributesAndroidInjector
    abstract fun contributeGalleryFragment(): GalleryFragment

    @ContributesAndroidInjector
    abstract fun contributeDetailFragment(): DetailFragment
}
