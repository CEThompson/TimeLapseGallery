package com.vwoom.timelapsegallery.weather

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ForecastLocationResult(
    @SerializedName("@id")
    @Expose
    var id: Any? = null,

    @SerializedName("@type")
    @Expose
    var type: Any? = null,

    @SerializedName("cwa")
    @Expose
    var cwa: Any? = null,

    @SerializedName("forecastOffice")
    @Expose
    var forecastOffice: Any? = null,

    @SerializedName("gridX")
    @Expose
    var gridX: Any? = null,

    @SerializedName("gridY")
    @Expose
    var gridY: Any? = null,

    @SerializedName("forecast")
    @Expose
    var forecast: Any? = null,

    @SerializedName("forecastHourly")
    @Expose
    var forecastHourly: Any? = null,

    @SerializedName("forecastGridData")
    @Expose
    var forecastGridData: Any? = null,

    @SerializedName("observationStations")
    @Expose
    var observationStations: Any? = null,

    @SerializedName("relativeLocation")
    @Expose
    var relativeLocation: RelativeLocation? = null,

    @SerializedName("forecastZone")
    @Expose
    var forecastZone: Any? = null,

    @SerializedName("county")
    @Expose
    var county: Any? = null,

    @SerializedName("fireWeatherZone")
    @Expose
    var fireWeatherZone: Any? = null,

    @SerializedName("timeZone")
    @Expose
    var timeZone: Any? = null,

    @SerializedName("radarStation")
    @Expose
    var radarStation: Any? = null
)