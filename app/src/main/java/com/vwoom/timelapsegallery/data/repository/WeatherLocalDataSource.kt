package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.weather.ForecastResponse

class WeatherLocalDataSource: WeatherDataSource {
    override fun getForecast(): ForecastResponse? {
        // TODO read forecast response from text file
        return null
    }
}