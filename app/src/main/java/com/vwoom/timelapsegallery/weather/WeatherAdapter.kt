package com.vwoom.timelapsegallery.weather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.databinding.DialogWeatherRecyclerviewItemBinding
import com.vwoom.timelapsegallery.weather.WeatherUtils.getTimestampForDayFromPeriod
import com.vwoom.timelapsegallery.weather.WeatherUtils.getWeatherType
import com.vwoom.timelapsegallery.weather.data.ForecastResponse


class WeatherAdapter : RecyclerView.Adapter<WeatherAdapter.WeatherAdapterViewHolder>() {

    private var days: List<ForecastDay> = emptyList()

    inner class WeatherAdapterViewHolder(val binding: DialogWeatherRecyclerviewItemBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherAdapterViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val shouldAttachToParentImmediately = false

        val binding = DialogWeatherRecyclerviewItemBinding.inflate(inflater, parent, shouldAttachToParentImmediately)
        return WeatherAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeatherAdapterViewHolder, position: Int) {
        val day = days[position].day
        val night = days[position].night

        val isF: Boolean = night?.temperatureUnit?.equals("F") ?: false

        val degree = if (isF) FAHRENHEIT else CELSIUS

        // Set info for the day
        holder.binding.periodName.text = day?.name ?: night?.name

        if (night == null) {
            holder.binding.night.detailItemLayout.visibility = View.GONE
        } else {
            // Bind info for night
            holder.binding.night.temperature.text = holder.itemView.context
                    .getString(R.string.degree, night.temperature, degree)
            holder.binding.night.weatherDetailItemLongDescription.text = night.detailedForecast
            holder.binding.night.windDirection.text = night.windDirection
            holder.binding.night.windSpeed.text = night.windSpeed
            holder.binding.night.description.text = night.shortForecast
            // Set the icon

            val type = getWeatherType(night.shortForecast)
            val nightIcon = WeatherUtils.getWeatherIconResource(
                    holder.itemView.context,
                    weatherType = type,
                    isDay = false,
                    timestamp = getTimestampForDayFromPeriod(night.startTime))
            holder.binding.night.icon.setImageDrawable(nightIcon)

            // Show the layout
            holder.binding.night.detailItemLayout.visibility = View.VISIBLE
        }
        // Bind info for day
        if (day == null) {
            holder.binding.day.detailItemLayout.visibility = View.GONE
        } else {
            // Bind the info for the day
            holder.binding.day.temperature.text = holder.itemView.context
                    .getString(R.string.degree, day.temperature, degree)
            holder.binding.day.weatherDetailItemLongDescription.text = day.detailedForecast
            holder.binding.day.windDirection.text = day.windDirection
            holder.binding.day.windSpeed.text = day.windSpeed
            holder.binding.day.description.text = day.shortForecast


            val type = getWeatherType(day.shortForecast)
            val dayIcon = WeatherUtils.getWeatherIconResource(
                    holder.itemView.context,
                    weatherType = type,
                    isDay = true,
                    timestamp = getTimestampForDayFromPeriod(day.startTime))
            // Set the icon
            holder.binding.day.icon.setImageDrawable(dayIcon)

            // Show the layout
            holder.binding.day.detailItemLayout.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return days.size
    }

    fun setWeatherData(data: List<ForecastResponse.Period>) {
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

            for (i in start..periods.size step 2) {
                if (i >= periods.size) break
                val day = periods[i]
                val night = if ((i + 1) < periods.size) periods[i + 1] else null
                result.add(ForecastDay(day, night))
            }
            return result.toList()
        }
    }
}