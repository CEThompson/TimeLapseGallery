package com.vwoom.timelapsegallery.utils

import com.vwoom.timelapsegallery.data.view.ProjectView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToLong

// TODO convert to kotlinx datetime
const val DAY_IN_MILLISECONDS: Long = (1000 * 60 * 60 * 24).toLong()

object TimeUtils {

    fun getExifDateTimeFromTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    fun getShortDateFromTimestamp(timestamp: Long): String {
        return SimpleDateFormat("M/d/y", Locale.getDefault()).format(Date(timestamp))
    }

    fun getDayFromTimestamp(timestamp: Long): String {
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
    }

    fun getDateFromTimestamp(timestamp: Long): String {
        return SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    fun getTimeFromTimestamp(timestamp: Long): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    fun getDaysSinceTimeStamp(timestampPhoto: Long, timestampNow: Long): Long {
        if (timestampNow < timestampPhoto) return -1 // ERROR CASE

        val photoStartOfDay = getTimestampStartOfDay(timestampPhoto)
        val nowStartOfDay = getTimestampStartOfDay(timestampNow)

        val days: Double = ((nowStartOfDay.toDouble() - photoStartOfDay.toDouble()) / DAY_IN_MILLISECONDS.toDouble())
        return days.roundToLong()
    }

    fun getTimestampStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar.timeInMillis = timestamp
        calendar[Calendar.HOUR] = 0
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        return calendar.timeInMillis
    }

    fun daysUntilDue(projectView: ProjectView): Long {
        val daysSinceLastPhotoTaken = getDaysSinceTimeStamp(projectView.cover_photo_timestamp, System.currentTimeMillis())
        val interval: Int = projectView.interval_days
        return interval - daysSinceLastPhotoTaken
    }

}