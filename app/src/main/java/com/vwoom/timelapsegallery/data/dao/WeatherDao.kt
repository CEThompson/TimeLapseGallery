package com.vwoom.timelapsegallery.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vwoom.timelapsegallery.data.entry.WeatherEntry

@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather WHERE id = 1")
    suspend fun getWeather(): WeatherEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weatherEntry: WeatherEntry)

}