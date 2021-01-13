package com.example.diap.common.dependencyinjection.service

import android.app.Service
import android.content.Context
import dagger.Module
import dagger.Provides

@Module
class ServiceModule(val service: Service) {
    @Provides
    fun context(): Context = service
}
