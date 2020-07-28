package com.vwoom.timelapsegallery

import android.app.Activity
import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.vwoom.timelapsegallery.di.AppInjector
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import javax.inject.Inject

// TODO: fix dagger injection or convert to koin
class TimeLapseGalleryApplication : Application(), CameraXConfig.Provider, HasActivityInjector {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Activity>

    override fun activityInjector(): AndroidInjector<Activity> = androidInjector

    override fun onCreate() {
        super.onCreate()
        AppInjector.init(this)
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }

    // Use this block for delayed launch
    /*override fun onCreate() {
        super.onCreate()
        delayedInit()
    }
    private val applicationScope = CoroutineScope(Dispatchers.Default)
    private fun delayedInit(){
        applicationScope.launch {
            // Late init application stuff here in order to prevent launch delay
        }
    }*/
}