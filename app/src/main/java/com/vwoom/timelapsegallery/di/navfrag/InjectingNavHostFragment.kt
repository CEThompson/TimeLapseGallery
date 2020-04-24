package com.vwoom.timelapsegallery.di.navfrag

import android.content.Context
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.vwoom.timelapsegallery.di.navfrag.InjectingFragmentFactory
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class InjectingNavHostFragment: NavHostFragment() {

    @Inject
    protected lateinit var daggerFragmentInjectionFactory: InjectingFragmentFactory

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        childFragmentManager.fragmentFactory = daggerFragmentInjectionFactory
        super.onCreate(savedInstanceState)

    }
}