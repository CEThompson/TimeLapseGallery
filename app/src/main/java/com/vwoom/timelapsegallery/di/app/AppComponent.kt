package com.vwoom.timelapsegallery.di.app

import com.example.diap.common.dependencyinjection.service.ServiceComponent
import com.example.diap.common.dependencyinjection.service.ServiceModule
import com.vwoom.timelapsegallery.di.activity.ActivityComponent
import com.vwoom.timelapsegallery.di.activity.ActivityModule
import dagger.Component

@AppScope
@Component(
        modules = [
            AppModule::class
        ]
)
interface AppComponent {
    fun newActivityComponentBuilder(): ActivityComponent.Builder
    fun newServiceComponent(serviceModule: ServiceModule): ServiceComponent
}