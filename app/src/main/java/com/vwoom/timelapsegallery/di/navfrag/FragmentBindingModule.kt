package com.vwoom.timelapsegallery.di.navfrag

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.vwoom.timelapsegallery.gallery.GalleryFragment
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class FragmentBindingModule {
    @Binds
    @IntoMap
    @FragmentKey(GalleryFragment::class)
    abstract fun bindGalleryFragment(galleryFragment: GalleryFragment): Fragment

    @Binds
    abstract fun bindFragmentFactory(factory: InjectingFragmentFactory): FragmentFactory

    // TODO set up other fragments from nav graph
}