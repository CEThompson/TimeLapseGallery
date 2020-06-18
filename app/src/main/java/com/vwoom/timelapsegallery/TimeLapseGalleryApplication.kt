package com.vwoom.timelapsegallery

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.vwoom.timelapsegallery.di.AppComponent
import com.vwoom.timelapsegallery.di.DaggerAppComponent
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

// TODO: fix dagger injection or convert to koin
class TimeLapseGalleryApplication : Application(), CameraXConfig.Provider, HasAndroidInjector {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }

    override fun onCreate() {
        val component: AppComponent = DaggerAppComponent.builder()
                .application(this)
                .context(this)
                .build()
        component.inject(this)

        super.onCreate()
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