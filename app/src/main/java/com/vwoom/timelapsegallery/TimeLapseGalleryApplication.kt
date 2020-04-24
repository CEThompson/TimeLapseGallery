package com.vwoom.timelapsegallery

import android.app.Activity
import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.vwoom.timelapsegallery.di.AppInjector
import dagger.android.AndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import javax.inject.Inject

class TimeLapseGalleryApplication : Application(),  CameraXConfig.Provider, HasActivityInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    override fun onCreate() {
        super.onCreate()
        AppInjector.init(this)
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }

    override fun activityInjector() = dispatchingAndroidInjector

}
