package com.vwoom.timelapsegallery.di.fragment

import androidx.fragment.app.Fragment
import com.vwoom.timelapsegallery.di.activity.BaseActivity
import com.vwoom.timelapsegallery.di.presentation.PresentationModule

open class BaseFragment : Fragment() {

    private val presentationComponent by lazy {
        (requireActivity() as BaseActivity).activityComponent
        .newPresentationComponent(
                PresentationModule(this)
        )
    }

    protected val injector get() = presentationComponent
}