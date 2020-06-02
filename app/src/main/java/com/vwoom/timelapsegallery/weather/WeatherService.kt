package com.vwoom.timelapsegallery.weather

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vwoom.timelapsegallery.weather.WeatherApi.moshi
import kotlinx.coroutines.Deferred
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

const val weatherServiceBaseUrl = "https://api.weather.gov/"

private val retrofit = Retrofit.Builder()
        .baseUrl(weatherServiceBaseUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

interface WeatherService {
    @GET("points/{latitude},{longitude}")
    suspend fun getForecastLocationAsync(@Path("latitude") latitude: String,
                                         @Path("longitude") longitude: String): Deferred<ForecastLocationResponse?>

    @GET
    suspend fun getForecastAsync(@Url stringUrl: String): Deferred<ForecastResponse?>

}

object WeatherApi {
    val weatherService: WeatherService by lazy {
        retrofit.create(WeatherService::class.java)
    }
    val moshi: Moshi by lazy {
        Moshi.Builder()
                .add(KotlinJsonAdapterFactory()).build()
    }
}