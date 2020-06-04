package com.vwoom.timelapsegallery.di

import android.app.Application
import android.content.Context
import com.vwoom.timelapsegallery.TimeLapseGalleryApplication
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
        modules = [
            AndroidInjectionModule::class,
            AndroidSupportInjectionModule::class,
            DataModule::class,
            GalleryModule::class,
            DetailModule::class
        ]
)
interface AppComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application (application: Application): Builder
        @BindsInstance
        fun context(context: Context): Builder
        fun build(): AppComponent
    }
    fun inject(app: TimeLapseGalleryApplication)
}