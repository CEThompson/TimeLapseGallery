package com.vwoom.timelapsegallery.weather

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

const val baseUrl = "https://api.weather.gov/"

interface WeatherService {
    @GET("points/{latitude},{longitude}")
    fun getForecastForLocation(@Path("latitude") latitude: String, @Path("longitude") longitude: String): Call<ForecastLocationResult>

    @GET
    fun getForecast(@Url stringUrl: String): String
}