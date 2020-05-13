package com.vwoom.timelapsegallery.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.vwoom.timelapsegallery.data.entry.WeatherEntry

@Dao
interface WeatherDao {
    /*@Query("SELECT * FROM weather WHERE id = 1")
    fun getWeatherLiveData(): LiveData<WeatherEntry?>*/

    @Query("SELECT * FROM weather WHERE id = 1")
    suspend fun getWeather(): WeatherEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weatherEntry: WeatherEntry)
/*
    @Delete
    fun deleteWeather(weatherEntry: WeatherEntry)

    @Update
    fun updateWeather(weatherEntry: WeatherEntry)*/
}