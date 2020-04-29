package com.vwoom.timelapsegallery.weather

import com.google.gson.annotations.SerializedName

data class ForecastLocationResponse(
    @SerializedName("@context")
    val context: List<Any>,
    val geometry: Geometry,
    val id: String,
    val properties: Properties,
    val type: String
) {

    data class Geometry(
            val coordinates: List<Double>,
            val type: String
    )

    data class Properties(
            @SerializedName("@id")
            val id: String,
            @SerializedName("@type")
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

    data class RelativeLocation(
            val geometry: GeometryX,
            val properties: PropertiesX,
            val type: String
    )

    data class GeometryX(
            val coordinates: List<Double>,
            val type: String
    )

    data class PropertiesX(
            val bearing: Bearing,
            val city: String,
            val distance: Distance,
            val state: String
    )

    data class Bearing(
            val unitCode: String,
            val value: Int
    )

    data class Distance(
            val unitCode: String,
            val value: Double
    )
}