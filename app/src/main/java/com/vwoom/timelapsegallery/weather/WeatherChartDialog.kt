package com.vwoom.timelapsegallery.weather

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.utils.TimeUtils
import java.text.DecimalFormat
import java.util.*

class WeatherChartDialog(context: Context): Dialog(context) {

    fun showCachedForecast(result: WeatherResult.CachedForecast<ForecastResponse>) {
        // Handle the check image
        this.findViewById<ImageView>(R.id.update_confirmation_image_view)?.setImageResource(R.drawable.ic_clear_red_24dp)
        this.findViewById<ImageView>(R.id.update_confirmation_image_view)?.visibility = View.VISIBLE

        // Show the time of the forecast
        this.findViewById<TextView>(R.id.update_time_tv)?.text =
                "Cached: ${TimeUtils.getDateFromTimestamp(result.timestamp)} ${TimeUtils.getTimeFromTimestamp(result.timestamp)}"
        this.findViewById<TextView>(R.id.update_time_tv)?.visibility = View.VISIBLE

        // Show reason for showing forecast that hasn't been updated today
        this.findViewById<TextView>(R.id.error_message_tv)?.text= "Showing cached data: ${result.message}"
        this.findViewById<TextView>(R.id.error_message_tv)?.visibility = View.VISIBLE

        // Hide the progress
        this.findViewById<ProgressBar>(R.id.weather_chart_progress)?.visibility = View.INVISIBLE

        // Show the chart
        this.findViewById<LineChart>(R.id.weather_chart)?.visibility = View.VISIBLE
    }

    fun showTodaysForecast(result: WeatherResult.TodaysForecast<ForecastResponse>){
        // Handle the check image
        this.findViewById<ImageView>(R.id.update_confirmation_image_view)?.setImageResource(R.drawable.ic_check_green_24dp)
        this.findViewById<ImageView>(R.id.update_confirmation_image_view)?.visibility = View.VISIBLE

        // Set and show the time the forecast was updated
        this.findViewById<TextView>(R.id.update_time_tv)?.text =
                "Updated Today: ${TimeUtils.getDateFromTimestamp(result.timestamp)} ${TimeUtils.getTimeFromTimestamp(result.timestamp)}"
        this.findViewById<TextView>(R.id.update_time_tv)?.visibility = View.VISIBLE

        // No error message shown
        this.findViewById<TextView>(R.id.error_message_tv)?.visibility = View.INVISIBLE

        // Show chart and hide loading progress
        this.findViewById<ProgressBar>(R.id.weather_chart_progress)?.visibility = View.INVISIBLE
        this.findViewById<LineChart>(R.id.weather_chart)?.visibility = View.VISIBLE
    }

    fun showUpdateSuccess(result: WeatherResult.UpdateForecast.Success<ForecastResponse>){
        // Bind and show the time of the update
        if (result.timestamp!=null)
            this.findViewById<TextView>(R.id.update_time_tv)?.text =
                    "Updated Today: ${TimeUtils.getDateFromTimestamp(result.timestamp)} ${TimeUtils.getTimeFromTimestamp(result.timestamp)}"
        this.findViewById<TextView>(R.id.update_time_tv)?.visibility = View.VISIBLE

        // Handle the check image
        this.findViewById<ImageView>(R.id.update_confirmation_image_view)?.setImageResource(R.drawable.ic_check_green_24dp)
        this.findViewById<ImageView>(R.id.update_confirmation_image_view)?.visibility = View.VISIBLE

        // Hide loading indicator and error, show the chart
        this.findViewById<TextView>(R.id.error_message_tv)?.visibility = View.INVISIBLE
        this.findViewById<ProgressBar>(R.id.weather_chart_progress)?.visibility = View.INVISIBLE
        this.findViewById<LineChart>(R.id.weather_chart)?.visibility = View.VISIBLE
    }

    fun showUpdateFailure(result: WeatherResult.UpdateForecast.Failure<ForecastResponse>){
        // Show the forecast time?
        this.findViewById<TextView>(R.id.update_time_tv)?.visibility = View.VISIBLE

        // Show the reason for the failure
        this.findViewById<TextView>(R.id.error_message_tv)?.visibility = View.VISIBLE
        this.findViewById<TextView>(R.id.error_message_tv)?.text = "Update failed: ${result.message}"

        // Handle the check image
        this.findViewById<ImageView>(R.id.update_confirmation_image_view)?.setImageResource(R.drawable.ic_clear_red_24dp)
        this.findViewById<ImageView>(R.id.update_confirmation_image_view)?.visibility = View.VISIBLE

        // Show the chart and hide the loading indicator
        this.findViewById<ProgressBar>(R.id.weather_chart_progress)?.visibility = View.INVISIBLE
        this.findViewById<LineChart>(R.id.weather_chart)?.visibility = View.VISIBLE
    }

    fun showWeatherLoading(){
        // Show the loading indicator
        this.findViewById<ProgressBar>(R.id.weather_chart_progress)?.visibility = View.VISIBLE
        //mWeatherDialog?.findViewById<ImageView>(R.id.update_confirmation_image_view)?.visibility = View.GONE
    }

    fun showWeatherNoData(result: WeatherResult.NoData){
        this.findViewById<TextView>(R.id.update_time_tv)?.text =
                "No forecast data available"
        //mWeatherDialog?.findViewById<TextView>(R.id.update_status_tv)?.text = "error"
        this.findViewById<TextView>(R.id.update_time_tv)?.visibility = View.INVISIBLE
        this.findViewById<TextView>(R.id.error_message_tv)?.text = "No data: ${result.exception?.localizedMessage}"
        this.findViewById<ProgressBar>(R.id.weather_chart_progress)?.visibility = View.INVISIBLE
        this.findViewById<LineChart>(R.id.weather_chart)?.visibility = View.VISIBLE
        this.findViewById<ImageView>(R.id.update_confirmation_image_view)?.visibility = View.GONE
    }

    // TODO calc projects due per day
    fun setWeatherChart(forecast: ForecastResponse){
        val periods : List<ForecastResponse.Period>? = forecast.properties.periods
        if (periods != null){

            val chart = this.findViewById<LineChart>(R.id.weather_chart) ?: return

            //periods = periods.subList(1,periods.size-1)
            // Set the entries for the chart
            val weatherEntries: ArrayList<Entry> = arrayListOf()
            val averages: ArrayList<Entry> = arrayListOf()
            val iconEntries : ArrayList<Entry> = arrayListOf()
            val axisLabels: ArrayList<String> = arrayListOf()

            // Set the weather and icons
            for (i in periods.indices) {
                weatherEntries.add(Entry(i.toFloat(), periods[i].temperature.toFloat()))

                // Handle icon per period
                // TODO adjust icons per weather type, clear, rainy, cloudy, etc.
                if (periods[i].isDaytime){
                    iconEntries.add(Entry(i.toFloat(), periods[i].temperature.toFloat()+5f,
                            ContextCompat.getDrawable(this.context,R.drawable.ic_wb_sunny_black_24dp)))
                } else {
                    iconEntries.add(Entry(i.toFloat(), periods[i].temperature.toFloat()+5f,
                            ContextCompat.getDrawable(this.context,R.drawable.ic_star_black_24dp)))
                }
            }

            // Handle averages
            val start = if (periods[0].isDaytime) 0 else 1
            for (i in start until periods.size-1 step 2){
                //if ( (i+1) !in periods.indices) break
                val avg = (periods[i].temperature.toFloat() + periods[i+1].temperature.toFloat()) / 2f
                averages.add(Entry((i.toFloat()+(i+1).toFloat())/2f, avg))
            }
            if (start == 1) {
                val first = (periods[0].temperature.toFloat() + periods[1].temperature.toFloat()) / 2f
                val entry = Entry(0.5f, first)
                //val entry = Entry(0.5f, averages[0].y)
                averages.add(0,entry)

                val last = (periods[periods.size-1].temperature.toFloat() + periods[periods.size-2].temperature.toFloat()) / 2f
                val lastEntry = Entry(((periods.size-1 + periods.size-2).toFloat() / 2f), last)
                //val lastEntry = Entry(((periods.size-1 + periods.size-2).toFloat() / 2f), averages.last().y)
                averages.add(lastEntry)
            }

            // Handle labels
            for (i in 0 until periods.size-1 step 1){
                // TODO: get day of period and convert to string
                axisLabels.add(periods[i].name.substring(0,3).toUpperCase(Locale.getDefault()))
            }

            // Set axis info
            val valueFormatter = object: ValueFormatter(){
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    //return axisLabels[value.toInt()]
                    return if (value.toInt() < axisLabels.size)
                        axisLabels[value.toInt()]
                    else ""
                }
            }

            // Set the chart characteristics
            chart.setTouchEnabled(false)
            chart.xAxis?.granularity = 1f
            chart.xAxis?.valueFormatter = valueFormatter

            // Hide axis lines
            chart.xAxis?.setDrawGridLines(false)
            chart.xAxis?.setDrawAxisLine(false)
            chart.axisRight?.isEnabled = false
            chart.axisLeft?.isEnabled = false
            chart.description?.isEnabled = false


            // Set the dataSet
            val tempType = if (periods[0].temperatureUnit == "F") WeatherAdapter.FAHRENHEIT else WeatherAdapter.CELSIUS
            val weatherDataSet = LineDataSet(weatherEntries, tempType)
            val iconDataSet = LineDataSet(iconEntries, "Weather Type")
            val avgDataSet = LineDataSet(averages, "Average Temp")
            avgDataSet.setDrawCircles(false)
            avgDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            avgDataSet.setDrawValues(false)

            weatherDataSet.enableDashedLine(0.8f,1f,0f)
            weatherDataSet.setDrawCircles(false)

            iconDataSet.setDrawIcons(true)
            iconDataSet.setDrawCircles(false)
            iconDataSet.setDrawValues(false)
            iconDataSet.enableDashedLine(0f,1f,0f)
            iconDataSet.color = ContextCompat.getColor(this.context, R.color.black)

            // Style the dataSet
            weatherDataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            weatherDataSet.cubicIntensity = .2f
            weatherDataSet.color = ContextCompat.getColor(this.context, R.color.colorAccent)

            val tempFormatter = object: ValueFormatter() {
                private val format = DecimalFormat("###,##0")
                override fun getPointLabel(entry: Entry?): String {
                    return format.format(entry?.y)
                }
            }
            weatherDataSet.valueFormatter = tempFormatter
            weatherDataSet.valueTextSize = 14f

            // Assign the data to the chart
            val lineData = LineData(weatherDataSet, iconDataSet, avgDataSet)
            chart.data = lineData
            chart.invalidate()

            this.findViewById<LineChart>(R.id.weather_chart)?.visibility = View.VISIBLE
            this.findViewById<ProgressBar>(R.id.weather_chart_progress)?.visibility = View.INVISIBLE
        }
    }
}