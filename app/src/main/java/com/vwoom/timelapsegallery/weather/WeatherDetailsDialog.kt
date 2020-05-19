package com.vwoom.timelapsegallery.weather

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vwoom.timelapsegallery.R

class WeatherDetailsDialog(context: Context): Dialog(context) {

    fun showWeatherLoading(){
        // Show the loading indicator
        this.findViewById<ProgressBar>(R.id.weather_details_progress)?.visibility = View.VISIBLE
        this.findViewById<RecyclerView>(R.id.weather_recycler_view)?.visibility = View.INVISIBLE
        //mWeatherDialog?.findViewById<ImageView>(R.id.update_confirmation_image_view)?.visibility = View.GONE
    }

    fun showWeatherData(){
        this.findViewById<ProgressBar>(R.id.weather_details_progress)?.visibility = View.INVISIBLE
        this.findViewById<RecyclerView>(R.id.weather_recycler_view)?.visibility = View.VISIBLE
    }

    private val mWeatherRecyclerView: RecyclerView
    private val mWeatherAdapter: WeatherAdapter

    init {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setContentView(R.layout.dialog_weather_details)
        mWeatherRecyclerView = this.findViewById(R.id.weather_recycler_view)
        mWeatherAdapter = WeatherAdapter()
        val weatherLayoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        mWeatherRecyclerView.apply {
            layoutManager = weatherLayoutManager
            setHasFixedSize(false)
            adapter = mWeatherAdapter
        }
    }

    fun handleWeatherResult(result: WeatherResult<ForecastResponse>){
        when (result){
            is WeatherResult.Loading -> this.showWeatherLoading()
            is WeatherResult.TodaysForecast -> {
                mWeatherAdapter.setWeatherData(result.data.properties.periods)
                showWeatherData()
            }
            // TODO convert error messages to string resources
            is WeatherResult.NoData -> {
                // TODO show no data
            }
            // TODO modify to clearly show that cached data is shown
            is WeatherResult.CachedForecast -> {
                mWeatherAdapter.setWeatherData(result.data.properties.periods)
                showWeatherData()
            }
            is WeatherResult.UpdateForecast.Failure -> {
                if (result.data != null)
                    mWeatherAdapter.setWeatherData(result.data.properties.periods)
                showWeatherData()
            }

            is WeatherResult.UpdateForecast.Success -> {
                if (result.data != null)
                    mWeatherAdapter.setWeatherData(result.data.properties.periods)
                showWeatherData()
            }

        }
    }

}