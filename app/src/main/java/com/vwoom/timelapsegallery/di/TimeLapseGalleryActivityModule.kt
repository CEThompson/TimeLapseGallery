package com.vwoom.timelapsegallery.di

import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

/*
@Suppress("unused")
//@Module(includes =[NavHostModule::class], subcomponents = [TimeLapseGalleryActivityComponent::class])
@Module(subcomponents = [TimeLapseGalleryActivityComponent::class])
abstract class TimeLapseGalleryActivityModule {
   */
/* @ContributesAndroidInjector(modules = [FragmentBuildersModule::class])
    abstract fun contributeTimeLapseGalleryActivity(): TimeLapseGalleryActivity*//*

    @Binds
    @IntoMap
    @ClassKey(TimeLapseGalleryActivity::class)
    abstract fun bindTimeLapseGalleryActivityFactory(factory: TimeLapseGalleryActivityComponent.Factory): AndroidInjector.Factory<*>
}*/
