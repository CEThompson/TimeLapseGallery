package com.vwoom.timelapsegallery.weather

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.weather.MoonPhaseCalculator.getMoonPhaseFromTimestamp
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

object WeatherUtils {

    enum class WeatherType {
        Snowy, Rainy, Cloudy, Foggy, Stormy, Clear
    }

    fun getWeatherIconResource(context: Context, isDay: Boolean, weatherType: WeatherType, timestamp: Long?): Drawable? {
        val resource = when (weatherType) {
            WeatherType.Cloudy -> R.drawable.ic_cloud_black_24dp
            WeatherType.Foggy -> R.drawable.ic_baseline_cloud_queue_24
            WeatherType.Stormy -> R.drawable.ic_baseline_flash_on_24
            WeatherType.Snowy -> R.drawable.ic_ac_unit_black_24dp
            WeatherType.Rainy -> R.drawable.ic_invert_colors_black_24dp
            WeatherType.Clear -> {
                when {
                    isDay -> R.drawable.ic_wb_sunny_black_24dp
                    timestamp == null -> R.drawable.ic_brightness_2_black_24dp
                    else -> {
                        when (getMoonPhaseFromTimestamp(timestamp)) {
                            NEW_MOON -> R.drawable.ic_star_black_24dp
                            FULL_MOON -> R.drawable.ic_brightness_1_black_24dp
                            else -> R.drawable.ic_brightness_2_black_24dp
                        }
                    }
                }
            }
        }
        return ContextCompat.getDrawable(context, resource)
    }

    // Returns a weather icon from a forecast period description
    fun getWeatherType(forecastDescription: String): WeatherType {
        val description = forecastDescription.toLowerCase(Locale.ROOT)
        // In order of severity
        return when {
            // If snow or ice
            description.contains("snow") ||
                    description.contains("ice") -> {
                WeatherType.Snowy
            }
            // Stormy
            description.contains("storm") -> {
                WeatherType.Stormy
            }
            // If rain or showers
            description.contains("rain") ||
                    description.contains("shower") -> {
                WeatherType.Rainy
            }
            // If cloudy
            description.contains("cloud") -> WeatherType.Cloudy
            // If foggy
            description.contains("fog") -> WeatherType.Foggy
            // Otherwise clear
            else -> WeatherType.Clear
        }
    }

    // Returns a timestamp from a forecast response period start time string
    fun getTimestampForDayFromPeriod(timeString: String): Long? {
        //if (period == null) return null
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val dateString = timeString.split("T")[0]
            val date = dateFormat.parse(dateString)
            date?.time
        } catch (e: Exception) {
            Timber.d("Error getting timestamp from period: ${e.message}")
            null
        }
    }
}

