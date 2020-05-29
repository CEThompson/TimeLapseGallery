package com.vwoom.timelapsegallery.data.entry

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather")
data class WeatherEntry(var forecastJsonString: String, var timestamp: Long) {
    @PrimaryKey
    var id = 1
}
