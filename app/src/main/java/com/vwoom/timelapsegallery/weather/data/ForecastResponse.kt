package com.vwoom.timelapsegallery.weather.data

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class ForecastResponse(
        @Json(name="@context")
    val context: List<Any>,
        val geometry: Geometry,
        val properties: Properties,
        val type: String
) {
    @Keep
    data class Geometry(
            val coordinates: List<List<List<Double>>>,
            val type: String
    )
    @Keep
    data class Properties(
            val elevation: Elevation,
            val forecastGenerator: String,
            val generatedAt: String,
            val periods: List<Period>,
            val units: String,
            val updateTime: String,
            val updated: String,
            val validTimes: String
    )
    @Keep
    data class Elevation(
            val unitCode: String,
            val value: Double
    )
    @Keep
    data class Period(
            val detailedForecast: String,
            val endTime: String,
            val icon: String,
            val isDaytime: Boolean,
            val name: String,
            val number: Int,
            val shortForecast: String,
            val startTime: String,
            val temperature: Int,
            val temperatureTrend: Any?,
            val temperatureUnit: String,
            val windDirection: String,
            val windSpeed: String
    )
}