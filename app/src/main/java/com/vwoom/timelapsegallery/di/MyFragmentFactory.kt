package com.vwoom.timelapsegallery.di.navfrag

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.vwoom.timelapsegallery.gallery.GalleryFragment
import java.lang.RuntimeException
import javax.inject.Inject
import javax.inject.Provider

/*
class MyFragmentFactory
@Inject constructor(
        private val mainFragProvider: Provider<GalleryFragment>
): FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when(className){
            GalleryFragment::class.java.canonicalName -> mainFragProvider.get()
            else -> mainFragProvider.get()
        }
    }

    private fun createFragmentAsFallback(classLoader: ClassLoader, className: String): Fragment {
        Log.d(TAG, "No creator found for class: $className. Using default constructor")
        return super.instantiate(classLoader, className)
    }

    companion object {
        private val TAG = InjectingFragmentFactory::class.simpleName
    }
}*/
