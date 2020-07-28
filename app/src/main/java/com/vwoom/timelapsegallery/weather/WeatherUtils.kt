package com.vwoom.timelapsegallery.weather

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.weather.MoonPhaseCalculator.getMoonPhaseFromTimestamp
import java.text.SimpleDateFormat
import java.util.*

object WeatherUtils {

    private val TAG = WeatherUtils::class.simpleName

    // Returns a weather icon from a forecast period description
    fun getWeatherIcon(context: Context, isDay: Boolean, forecastDescription: String, timestamp: Long?): Drawable? {
        val description = forecastDescription.toLowerCase(Locale.ROOT)
        val drawableInt: Int = when {
            // If cloudy or foggy
            description.contains("cloud") ||
                    description.contains("fog") -> {
                R.drawable.ic_cloud_black_24dp
            }
            // If rain or showers
            description.contains("rain") ||
                    description.contains("shower") -> {
                R.drawable.ic_invert_colors_black_24dp
            }
            // If snow or ice
            description.contains("snow") ||
                    description.contains("ice") -> {
                R.drawable.ic_invert_colors_black_24dp
            }
            // Otherwise clear
            else -> {
                if (isDay) R.drawable.ic_wb_sunny_black_24dp
                // If night AND its clear show full moons, new moons, or intermediate state
                else {
                    if (timestamp == null) R.drawable.ic_brightness_2_black_24dp
                    else when (getMoonPhaseFromTimestamp(timestamp)){
                        NEW_MOON -> R.drawable.ic_star_black_24dp
                        FULL_MOON -> R.drawable.ic_brightness_1_black_24dp
                        else -> R.drawable.ic_brightness_2_black_24dp
                    }
                }
            }
        }
        return ContextCompat.getDrawable(context, drawableInt)
    }

    // Returns a timestamp from a forecast response period start time string
    fun getTimestampFromPeriod(period: ForecastResponse.Period?): Long? {
        if (period == null) return null
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val dateString = period.startTime.split("T")[0]
            val date = dateFormat.parse(dateString)
            date?.time
        } catch (e: Exception) {
            Log.d(TAG, "Error getting timestamp from period: ${e.message}")
            null
        }
    }
}

