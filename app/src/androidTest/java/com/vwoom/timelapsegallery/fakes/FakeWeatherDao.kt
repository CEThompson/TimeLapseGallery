package com.vwoom.timelapsegallery.fakes

import com.vwoom.timelapsegallery.data.dao.WeatherDao
import com.vwoom.timelapsegallery.data.entry.WeatherEntry


class FakeWeatherDao: WeatherDao {
    var weatherEntry: WeatherEntry? = null

    override suspend fun getWeather(): WeatherEntry? {
        return weatherEntry
    }

    override suspend fun insertWeather(weatherEntry: WeatherEntry) {
        this.weatherEntry = weatherEntry
    }

    override suspend fun deleteWeather() {
        weatherEntry = null
    }

}