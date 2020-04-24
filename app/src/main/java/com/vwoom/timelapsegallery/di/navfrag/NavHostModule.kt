package com.vwoom.timelapsegallery.di.navfrag

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class NavHostModule {
    @ContributesAndroidInjector(modules = [FragmentBindingModule::class])
    abstract fun navHostFragmentInjector(): InjectingNavHostFragment
}