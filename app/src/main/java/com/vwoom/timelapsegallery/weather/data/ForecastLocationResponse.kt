package com.vwoom.timelapsegallery.weather.data

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class ForecastLocationResponse(
        @Json(name="@context")
    val context: List<Any>,
        val geometry: Geometry,
        val id: String,
        val properties: Properties,
        val type: String
) {
    @Keep
    data class Geometry(
            val coordinates: List<Double>,
            val type: String
    )
    @Keep
    data class Properties(
            @Json(name="@id")
            val id: String,
            @Json(name="@type")
            val type: String,
            val county: String,
            val cwa: String,
            val fireWeatherZone: String,
            val forecast: String,
            val forecastGridData: String,
            val forecastHourly: String,
            val forecastOffice: String,
            val forecastZone: String,
            val gridX: Int,
            val gridY: Int,
            val observationStations: String,
            val radarStation: String,
            val relativeLocation: RelativeLocation,
            val timeZone: String
    )
    @Keep
    data class RelativeLocation(
            val geometry: GeometryX,
            val properties: PropertiesX,
            val type: String
    )
    @Keep
    data class GeometryX(
            val coordinates: List<Double>,
            val type: String
    )
    @Keep
    data class PropertiesX(
            val bearing: Bearing,
            val city: String,
            val distance: Distance,
            val state: String
    )
    @Keep
    data class Bearing(
            val unitCode: String,
            val value: Int
    )
    @Keep
    data class Distance(
            val unitCode: String,
            val value: Double
    )
}