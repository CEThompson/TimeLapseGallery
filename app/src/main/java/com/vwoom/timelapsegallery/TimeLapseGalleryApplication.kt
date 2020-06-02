package com.vwoom.timelapsegallery

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

class TimeLapseGalleryApplication : Application(), CameraXConfig.Provider {
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