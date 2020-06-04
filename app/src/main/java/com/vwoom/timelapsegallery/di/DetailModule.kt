package com.vwoom.timelapsegallery.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.repository.TagRepository
import com.vwoom.timelapsegallery.detail.DetailFragment
import com.vwoom.timelapsegallery.detail.DetailViewModel
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import javax.inject.Provider

@Module
abstract class DetailModule {
    @Suppress("unused")
    @ContributesAndroidInjector
    abstract fun bind(): DetailFragment

    @Module
    class InjectViewModel {
        @Provides
        fun provideDetailViewModelFactory(
                providers: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
        ): ViewModelProvider.Factory = ViewModelFactory(providers)

        @Provides
        @IntoMap
        @ViewModelKey(DetailViewModel::class)
        fun provideDetailViewModel(projectRepository: ProjectRepository, tagRepository: TagRepository):
                ViewModel = DetailViewModel(projectRepository, tagRepository)
    }
}