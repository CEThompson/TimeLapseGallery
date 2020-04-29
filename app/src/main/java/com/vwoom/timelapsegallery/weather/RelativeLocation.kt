package com.vwoom.timelapsegallery.weather

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class RelativeLocation (
    @SerializedName("city")
    @Expose
    var city: Any? = null,

    @SerializedName("state")
    @Expose
    var state: Any? = null,

    @SerializedName("distance")
    @Expose
    var distance: Any? = null,

    @SerializedName("bearing")
    @Expose
    var bearing: Any? = null
)