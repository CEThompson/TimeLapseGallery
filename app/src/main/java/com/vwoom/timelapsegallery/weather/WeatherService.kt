package com.vwoom.timelapsegallery.weather

import com.vwoom.timelapsegallery.weather.data.ForecastLocationResponse
import com.vwoom.timelapsegallery.weather.data.ForecastResponse
import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

const val weatherServiceBaseUrl = "https://api.weather.gov/"

interface WeatherService {
    @GET("points/{latitude},{longitude}")
    fun getForecastLocationAsync(@Path("latitude") latitude: String,
                                 @Path("longitude") longitude: String): Deferred<ForecastLocationResponse?>

    @GET
    fun getForecastAsync(@Url stringUrl: String): Deferred<ForecastResponse?>

}
