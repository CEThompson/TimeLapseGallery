package com.vwoom.timelapsegallery.di.app

import com.vwoom.timelapsegallery.di.service.ServiceModule
import com.vwoom.timelapsegallery.di.activity.ActivityComponent
import com.vwoom.timelapsegallery.di.service.ServiceComponent
import dagger.Component

@AppScope
@Component(
        modules = [
            AppModule::class,
            DatabaseModule::class,
            NetworkModule::class
        ]
)
interface AppComponent {
    fun newActivityComponentBuilder(): ActivityComponent.Builder
    fun newServiceComponent(serviceModule: ServiceModule): ServiceComponent
}