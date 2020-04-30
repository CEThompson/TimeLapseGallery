package com.vwoom.timelapsegallery.weather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vwoom.timelapsegallery.databinding.DialogWeatherRecyclerviewItemBinding

class WeatherAdapter(private val mClickHandler: WeatherAdapterOnClickHandler) : RecyclerView.Adapter<WeatherAdapter.WeatherAdapterViewHolder>() {

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

        holder.binding.periodName.text = periods!![position].name
    }

    override fun getItemCount(): Int {
        return periods?.size ?: 0
    }

    fun setWeatherData(data: List<ForecastResponse.Period>){
        periods = data
        notifyDataSetChanged()
    }

}