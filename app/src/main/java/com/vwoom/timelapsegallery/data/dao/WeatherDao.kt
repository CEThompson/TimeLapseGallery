package com.vwoom.timelapsegallery.data.dao

import androidx.room.*
import com.vwoom.timelapsegallery.data.entry.WeatherEntry

@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather WHERE id = 1")
    suspend fun getWeather(): WeatherEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weatherEntry: WeatherEntry)

    @Query("DELETE FROM weather")
    suspend fun deleteWeather()
}