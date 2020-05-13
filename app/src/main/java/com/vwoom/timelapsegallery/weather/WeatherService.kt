package com.vwoom.timelapsegallery.weather

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

const val weatherServiceBaseUrl = "https://api.weather.gov/"

interface WeatherService {
    @GET("points/{latitude},{longitude}")
    suspend fun getForecastLocation(@Path("latitude") latitude: String,
                                    @Path("longitude") longitude: String): ForecastLocationResponse?

    @GET
    suspend fun getForecast(@Url stringUrl: String): ForecastResponse?

}