package com.vwoom.timelapsegallery.di.activity

import androidx.appcompat.app.AppCompatActivity
import com.vwoom.timelapsegallery.TimeLapseGalleryApplication
import com.vwoom.timelapsegallery.di.presentation.PresentationModule

open class BaseActivity : AppCompatActivity() {

    private val appComponent get() = (application as TimeLapseGalleryApplication).appComponent

    val activityComponent by lazy {
        appComponent.newActivityComponentBuilder()
                .activity(this)
                .build()
    }

    private val presentationComponent by lazy {
        activityComponent.newPresentationComponent(
                PresentationModule(this)
        )
    }

    protected val injector get() = presentationComponent

}