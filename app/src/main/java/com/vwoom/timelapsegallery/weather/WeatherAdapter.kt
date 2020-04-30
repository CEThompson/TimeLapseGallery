package com.vwoom.timelapsegallery.weather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vwoom.timelapsegallery.databinding.DialogWeatherRecyclerviewItemBinding

class WeatherAdapter(private val mClickHandler: WeatherAdapterOnClickHandler) : RecyclerView.Adapter<WeatherAdapter.WeatherAdapterViewHolder>() {

    // TODO process periods into weather per day (split into day and evening)
    private var periods: List<ForecastResponse.Period>? = null

    interface WeatherAdapterOnClickHandler {
        fun onClick(clickedPeriod: ForecastResponse.Period)
    }

    inner class WeatherAdapterViewHolder(val binding: DialogWeatherRecyclerviewItemBinding)
        : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        override fun onClick(view: View) {
            val adapterPosition = adapterPosition
            val clickedPeriod = periods!![adapterPosition]
            mClickHandler.onClick(clickedPeriod)
        }

        init {
            binding.root.setOnClickListener(this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherAdapterViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val shouldAttachToParentImmediately = false

        val binding = DialogWeatherRecyclerviewItemBinding.inflate(inflater, parent, shouldAttachToParentImmediately)
        return WeatherAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeatherAdapterViewHolder, position: Int) {
        //TODO("Not yet implemented")

        if (periods!=null){
            val period = periods!![position]

            holder.binding.periodName.text = period.name
            holder.binding.temperature.text = period.temperature.toString()
            holder.binding.temperatureUnit.text = period.temperatureUnit
            holder.binding.windDirection.text = period.windDirection
            holder.binding.windSpeed.text = period.windSpeed

            // TODO implement my own weather icons
            /*
            Glide.with(holder.itemView.context)
                    .load(period.icon).into(holder.binding.icon)
             */
        }


    }

    override fun getItemCount(): Int {
        return periods?.size ?: 0
    }

    fun setWeatherData(data: List<ForecastResponse.Period>){
        periods = data
        notifyDataSetChanged()
    }

}