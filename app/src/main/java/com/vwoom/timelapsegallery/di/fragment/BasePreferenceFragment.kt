package com.vwoom.timelapsegallery.di.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.vwoom.timelapsegallery.di.activity.BaseActivity
import com.vwoom.timelapsegallery.di.presentation.PresentationModule

open class BasePreferenceFragment: PreferenceFragmentCompat() {

    private val presentationComponent by lazy {
        (requireActivity() as BaseActivity).activityComponent
                .newPresentationComponent(
                        PresentationModule(this)
                )
    }

    protected val injector get() = presentationComponent

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // TODO: Implement in subclass?
    }
}