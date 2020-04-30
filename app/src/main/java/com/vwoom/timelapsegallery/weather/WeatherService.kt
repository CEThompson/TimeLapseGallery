package com.vwoom.timelapsegallery.weather

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

const val weatherServiceBaseUrl = "https://api.weather.gov/"

interface WeatherService {
    @GET("points/{latitude},{longitude}")
    fun getForecastLocation(@Path("latitude") latitude: String, @Path("longitude") longitude: String): Call<ForecastLocationResponse>

    @GET
    fun getForecast(@Url stringUrl: String): Call<ForecastResponse>
}