package com.vwoom.timelapsegallery.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vwoom.timelapsegallery.detail.DetailViewModel
import com.vwoom.timelapsegallery.gallery.GalleryViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(GalleryViewModel::class)
    abstract fun bindGalleryViewModel(galleryViewModel: GalleryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DetailViewModel::class)
    abstract fun bindDetailViewModel(detailViewModel: DetailViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: TlgViewModelFactory): ViewModelProvider.Factory
}