package com.vwoom.timelapsegallery.weather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.databinding.DialogWeatherRecyclerviewItemBinding


class WeatherAdapter() : RecyclerView.Adapter<WeatherAdapter.WeatherAdapterViewHolder>() {


    //private var periods: List<ForecastResponse.Period> = emptyList()

    private var days: List<ForecastDay> = emptyList()

    /*interface WeatherAdapterOnClickHandler {
        fun onClick(clickedDay: ForecastDay)
    }*/

    inner class WeatherAdapterViewHolder(val binding: DialogWeatherRecyclerviewItemBinding)
        : RecyclerView.ViewHolder(binding.root)//, View.OnClickListener {
        /*override fun onClick(view: View) {
            val adapterPosition = adapterPosition
            val clickedDay = days[adapterPosition]
            //mClickHandler.onClick(clickedDay)
        }*/

        /*init {
            binding.root.setOnClickListener(this)
        }*/
    //}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherAdapterViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val shouldAttachToParentImmediately = false

        val binding = DialogWeatherRecyclerviewItemBinding.inflate(inflater, parent, shouldAttachToParentImmediately)
        return WeatherAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeatherAdapterViewHolder, position: Int) {
        //TODO("Not yet implemented")
        val day = days[position].day
        val night = days[position].night

        var isF: Boolean = night?.temperatureUnit?.equals("F") ?: false

        var degree = if (isF) FAHRENHEIT else CELSIUS

        // Set info for the day
        holder.binding.periodName.text = day?.name ?: night?.name

        if (night == null) {
            holder.binding.night.nightLayout.visibility = View.GONE
        } else {
            // Bind info for night
            holder.binding.night.temperature.text = holder.itemView.context
                    .getString(R.string.degree, night.temperature, degree)
            //holder.binding.night.temperatureTrend.text = night?.temperatureTrend.toString()
            holder.binding.night.windDirection.text = night.windDirection
            holder.binding.night.windSpeed.text = night.windSpeed
            holder.binding.night.description.text = night.shortForecast
        }
        // Bind info for day
        if (day == null) {
            // TODO handle blank day
            holder.binding.day.dayLayout.visibility = View.GONE
        } else {
            holder.binding.day.temperature.text = holder.itemView.context
                    .getString(R.string.degree, day.temperature, degree)
            //holder.binding.day.temperatureTrend.text = day.temperatureTrend.toString()
            holder.binding.day.windDirection.text = day.windDirection
            holder.binding.day.windSpeed.text = day.windSpeed
            holder.binding.day.description.text = day.shortForecast
            holder.binding.day.dayLayout.visibility = View.VISIBLE
        }

        /*holder.binding.periodName.text = period.name
        holder.binding.temperature.text = period.temperature.toString()
        holder.binding.temperatureUnit.text = period.temperatureUnit
        holder.binding.windDirection.text = period.windDirection
        holder.binding.windSpeed.text = period.windSpeed*/

        // TODO implement my own weather icons
        /*
        Glide.with(holder.itemView.context)
                .load(period.icon).into(holder.binding.icon)
         */



    }

    override fun getItemCount(): Int {
        return days.size
    }

    fun setWeatherData(data: List<ForecastResponse.Period>){
        //periods = data
        days = convertPeriodsToForecastDays(data)
        notifyDataSetChanged()
    }

    data class ForecastDay(
            var day: ForecastResponse.Period?,
            var night: ForecastResponse.Period?
    )

    companion object {
        const val CELSIUS = "\u2103"
        const val FAHRENHEIT = "\u2109"

        fun convertPeriodsToForecastDays(periods: List<ForecastResponse.Period>): List<ForecastDay> {
            val result = arrayListOf<ForecastDay>()

            if (!periods.first().isDaytime) {
                val night = periods.first()
                result.add(ForecastDay(null, night))
            }

            val start = if (periods.first().isDaytime) 0 else 1

            for (i in start..periods.size step 2){
                if (i>=periods.size) break
                val day = periods[i]
                val night = if ( (i+1) < periods.size) periods[i+1] else null
                result.add(ForecastDay(day, night))
            }
            return result.toList()
        }

    }
}