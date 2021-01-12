package com.vwoom.timelapsegallery.di.app

import android.app.Application
import com.vwoom.timelapsegallery.TimeLapseGalleryApplication
import com.vwoom.timelapsegallery.di.TimeLapseGalleryActivityModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@AppScope
@Component(
        modules = [
            AndroidInjectionModule::class,
            AndroidSupportInjectionModule::class,
            AppModule::class,
            TimeLapseGalleryActivityModule::class
        ]
)
interface AppComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder
        fun build(): AppComponent
    }

    fun inject(app: TimeLapseGalleryApplication)
}