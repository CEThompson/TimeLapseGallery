package com.vwoom.timelapsegallery.data.repository

import android.text.format.DateUtils
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.weather.ForecastResponse
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class WeatherLocalDataSource {
    fun getForecast(externalFilesDir: File): ForecastResponse? {
        // TODO read forecast response from text file
        // See if the forecast has been saved to text already for today
        val metaDir = File(externalFilesDir, FileUtils.META_FILE_SUBDIRECTORY)
        val weatherFile = File(metaDir, FileUtils.WEATHER_RESPONSE_TEXT_FILE)

        Log.d(TAG, "getting forecast from weather file")

        if (weatherFile.exists()) {
            Log.d(TAG, "local data source has local response")
            val inputAsString = FileInputStream(weatherFile).bufferedReader().use { it.readText() }
            Log.d(TAG, "input from text is $inputAsString")

            var localResponse: ForecastResponse? = null
            try {
                localResponse = Gson().fromJson(inputAsString, ForecastResponse::class.java)
                Log.d(TAG, localResponse.toString())
            } catch (e: JsonSyntaxException) {
                Log.d(TAG, "error getting json from weather file ${e.message}")
            }

            //Log.d("WeatherRepository", "local response is : $localResponse")
            // TODO figure out if time is from today: if so return the forecast
            if (localResponse!=null) {
                val forecastTime = localResponse.properties.validTimes
                if (!isForecastTimeToday(forecastTime)){
                    localResponse = null
                }
            }

            return localResponse
        }

        return null
    }

    private fun isForecastTimeToday(time: String): Boolean {
        return try {
            val dateString = time.split('T')[0]
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date: Date? = dateFormat.parse(dateString)
            val isToday = DateUtils.isToday(date!!.time)
            Log.d(TAG, "determining if time: $time is today: $isToday")
            DateUtils.isToday(date.time)
        } catch (e: Exception){
            Log.d(TAG, "exception thrown: ${e.message}")
            false
        }
    }

    companion object {
        private val TAG = WeatherLocalDataSource::class.simpleName
    }
}