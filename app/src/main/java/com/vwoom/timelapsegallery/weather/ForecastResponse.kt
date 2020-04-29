package com.vwoom.timelapsegallery.weather

import com.google.gson.annotations.SerializedName

data class ForecastResponse(
    @SerializedName("@context")
    val context: List<Any>,
    val geometry: Geometry,
    val properties: Properties,
    val type: String
) {

    data class Geometry(
            val geometries: List<GeometryX>,
            val type: String
    )

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

    data class GeometryX(
            val coordinates: List<Any>,
            val type: String
    )

    data class Elevation(
            val unitCode: String,
            val value: Double
    )

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
            val temperatureTrend: Any,
            val temperatureUnit: String,
            val windDirection: String,
            val windSpeed: String
    )
}