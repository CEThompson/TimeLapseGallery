package com.vwoom.timelapsegallery.weather

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.gallery.GalleryViewModel
import com.vwoom.timelapsegallery.weather.data.ForecastResponse

class WeatherDetailsDialog(context: Context, galleryViewModel: GalleryViewModel): Dialog(context) {

    private val weatherRecyclerView: RecyclerView
    private val weatherAdapter: WeatherAdapter

    init {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setContentView(R.layout.dialog_weather_details)
        this.setOnCancelListener { galleryViewModel.weatherDetailsDialogShowing = false }
        this.findViewById<FloatingActionButton>(R.id.weather_details_dialog_exit_fab).setOnClickListener {
            this.cancel()
        }
        
        weatherRecyclerView = this.findViewById(R.id.weather_recycler_view)
        weatherAdapter = WeatherAdapter()
        val weatherLayoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        weatherRecyclerView.apply {
            layoutManager = weatherLayoutManager
            setHasFixedSize(false)
            adapter = weatherAdapter
        }

        // Constrain the size of the chart to 80% of the smallest dimension
        val dm = context.resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels
        val minSize = (width.coerceAtMost(height) * 0.7).toInt()
        this.findViewById<RecyclerView>(R.id.weather_recycler_view)?.layoutParams?.width = minSize

        // Set the data
        if (galleryViewModel.weather.value != null) handleWeatherResult(galleryViewModel.weather.value!!)
    }

    private fun showWeatherLoading(){
        // Show the loading indicator
        this.findViewById<ProgressBar>(R.id.weather_details_progress)?.visibility = View.VISIBLE
        this.findViewById<RecyclerView>(R.id.weather_recycler_view)?.visibility = View.INVISIBLE
    }

    private fun showWeatherData(){
        this.findViewById<ProgressBar>(R.id.weather_details_progress)?.visibility = View.INVISIBLE
        this.findViewById<RecyclerView>(R.id.weather_recycler_view)?.visibility = View.VISIBLE
    }

    fun handleWeatherResult(result: WeatherResult<ForecastResponse>){
        when (result){
            is WeatherResult.Loading -> this.showWeatherLoading()
            is WeatherResult.TodaysForecast -> {
                weatherAdapter.setWeatherData(result.data.properties.periods)
                showWeatherData()
            }
            is WeatherResult.NoData -> {
                // No need to handle: details cannot be launched with no data state
            }
            is WeatherResult.CachedForecast -> {
                weatherAdapter.setWeatherData(result.data.properties.periods)
                showWeatherData()
            }
        }
    }

}