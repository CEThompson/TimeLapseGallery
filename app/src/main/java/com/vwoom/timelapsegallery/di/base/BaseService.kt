package com.vwoom.timelapsegallery.di.base

import android.app.Service
import com.example.diap.common.dependencyinjection.service.ServiceModule
import com.vwoom.timelapsegallery.TimeLapseGalleryApplication

// Note: currently base service unused
abstract class BaseService : Service() {

    private val appComponent get() = (application as TimeLapseGalleryApplication).appComponent

    val serviceComponent by lazy {
        appComponent.newServiceComponent(ServiceModule(this))
    }

}