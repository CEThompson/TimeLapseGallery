package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.weather.ForecastLocationResponse
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherService
import com.vwoom.timelapsegallery.weather.weatherServiceBaseUrl
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRemoteDataSource: WeatherDataSource {

    override fun getForecast(): ForecastResponse? {
        TODO("Not yet implemented")
    }
    /*private val retrofit = Retrofit.Builder()
            .baseUrl(weatherServiceBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    private val weatherService = retrofit.create(WeatherService::class.java)

    override fun getForecast(): ForecastResponse? {

        val url = getForecastLocation(latitude, longitude)?.properties?.forecast.toString()
        val forecastResponseCall: Call<ForecastResponse> = weatherService.getForecast(url)
        forecastResponseCall.enqueue(object: Callback<ForecastResponse> {
            override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                // TODO handle failure case
            }

            override fun onResponse(call: Call<ForecastResponse>, response: Response<ForecastResponse>) {
                val forecast: ForecastResponse? = response.body()
                return forecast
            }
        })
    }

    private fun getForecastLocation(latitude: String, longitude: String): ForecastLocationResponse? {
        val forecastCall: Call<ForecastLocationResponse> = weatherService
                .getForecastLocation(latitude = latitude, longitude = longitude)
        forecastCall.enqueue(object : Callback<ForecastLocationResponse> {
            override fun onFailure(call: Call<ForecastLocationResponse>, t: Throwable) {
                // TODO handle fail case
                return null
            }
            override fun onResponse(call: Call<ForecastLocationResponse>, response: Response<ForecastLocationResponse>) {
                val result: ForecastLocationResponse? = response.body()
                val forecastUrl = result?.properties?.forecast.toString()
                return result
            }
        })
    }*/

}