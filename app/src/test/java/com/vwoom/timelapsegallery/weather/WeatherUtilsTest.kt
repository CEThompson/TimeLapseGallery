package com.vwoom.timelapsegallery.weather

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class WeatherUtilsTest {

    @Test
    fun getWeatherType_whenRainy_returnsRain() {
        var forecastDescription = "Cloudy with a chance of showers"
        var type = WeatherUtils.getWeatherType(forecastDescription)
        assertTrue(type == WeatherUtils.WeatherType.Rainy)

        forecastDescription = "Light Rain"
        type = WeatherUtils.getWeatherType(forecastDescription)
        assertTrue(type == WeatherUtils.WeatherType.Rainy)
    }

    @Test
    fun getWeatherType_whenStormyAndRainy_returnsStormy() {
        val forecastDescription = "Showers And Thunderstorms Likely"
        val type = WeatherUtils.getWeatherType(forecastDescription)
        assertTrue(type == WeatherUtils.WeatherType.Stormy)
    }

    @Test
    fun getWeatherType_whenFoggy_returnsFoggy() {
        val forecastDescription = "Patchy Fog"
        val type = WeatherUtils.getWeatherType(forecastDescription)
        assertTrue(type == WeatherUtils.WeatherType.Foggy)
    }

    @Test
    fun getWeatherType_whenSnowy_returnsSnow() {
        val forecastDescription = "Windy with a chance of snow"
        val type = WeatherUtils.getWeatherType(forecastDescription)
        assertTrue(type == WeatherUtils.WeatherType.Snowy)
    }

    @Test
    fun getWeatherType_whenClear_returnsClear() {
        var forecastDescription = "Sunny"
        var type = WeatherUtils.getWeatherType(forecastDescription)
        assertTrue(type == WeatherUtils.WeatherType.Clear)

        forecastDescription = "Mostly clear"
        type = WeatherUtils.getWeatherType(forecastDescription)
        assertTrue(type == WeatherUtils.WeatherType.Clear)
    }

    @Test
    fun getTimestampFromPeriod_whenTimestampBelongsToDay_returnsTimestampOfSameDay() {
        // Given a period with a start time of
        val time = "2020-07-31T01:00:00-04:00"
        // "2020-07-31T01:00:00-04:00" or july 31 202

        // When we call the function to get its timestamp
        val timestamp = WeatherUtils.getTimestampForDayFromPeriod(time)
        assertTrue(timestamp != null)
        // Then a correct timestamp is returned for July 31, 2020
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp!!

        assertTrue(calendar[Calendar.MONTH] == Calendar.JULY)
        assertTrue(calendar[Calendar.YEAR] == 2020)
        assertTrue(calendar[Calendar.DAY_OF_MONTH] == 31)
    }
}