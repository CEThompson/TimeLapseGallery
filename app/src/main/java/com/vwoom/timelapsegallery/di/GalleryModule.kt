package com.vwoom.timelapsegallery.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.repository.TagRepository
import com.vwoom.timelapsegallery.gallery.GalleryFragment
import com.vwoom.timelapsegallery.gallery.GalleryViewModel
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import javax.inject.Provider

@Module
abstract class GalleryModule {
    @Suppress("unused")
    @ContributesAndroidInjector
    abstract fun bind(): GalleryFragment

    @Module
    class InjectViewModel {
        @Provides
        fun provideGalleryViewModelFactory(
                providers: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
        ) : ViewModelProvider.Factory = ViewModelFactory(providers)

        @Provides
        @IntoMap
        @ViewModelKey(GalleryViewModel::class)
        fun provideGalleryViewModel(projectRepository: ProjectRepository, tagRepository: TagRepository):
                ViewModel = GalleryViewModel(projectRepository, tagRepository)
    }
}