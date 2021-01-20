package com.vwoom.timelapsegallery.di.app

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vwoom.timelapsegallery.weather.WeatherService
import com.vwoom.timelapsegallery.weather.weatherServiceBaseUrl
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
class NetworkModule {

    @Provides
    @AppScope
    fun retrofit(moshi: Moshi): Retrofit {
        return Retrofit.Builder()
                .baseUrl(weatherServiceBaseUrl)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .build()
    }

    @Provides
    @AppScope
    fun weatherService(retrofit: Retrofit): WeatherService = retrofit.create(WeatherService::class.java)

    @Provides
    @AppScope
    fun moshi(): Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

}