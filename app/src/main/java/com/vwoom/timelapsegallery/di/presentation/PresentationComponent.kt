package com.vwoom.timelapsegallery.di.presentation

import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.camera2.Camera2Fragment
import com.vwoom.timelapsegallery.detail.DetailFragment
import com.vwoom.timelapsegallery.gallery.GalleryFragment
import com.vwoom.timelapsegallery.settings.SettingsFragment
import dagger.Subcomponent

@PresentationScope
@Subcomponent(modules = [PresentationModule::class, ViewModelModule::class])
interface PresentationComponent {
    fun inject(activity: TimeLapseGalleryActivity)
    fun inject(fragment: Camera2Fragment)
    fun inject(fragment: DetailFragment)
    fun inject(fragment: GalleryFragment)
    fun inject(fragment: SettingsFragment)
}