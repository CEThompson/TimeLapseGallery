package com.vwoom.timelapsegallery.utils

import com.vwoom.timelapsegallery.data.view.ProjectView
import java.text.SimpleDateFormat
import java.util.*

// TODO: (when stable) convert to kotlinx datetime
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

    // Returns the number of days since a photo's timestamp and now
    // Should account for long periods of time including leap years
    fun getDaysSinceTimeStamp(timestampPhoto: Long, timestampNow: Long): Long {
        if (timestampNow < timestampPhoto) return -1 // Error case: current time cannot be before photo

        // Convert both timestamps to the start of their respective days
        val photoStartOfDay = getTimestampStartOfDay(timestampPhoto)
        val nowStartOfDay = getTimestampStartOfDay(timestampNow)

        // Use calendar to get the day for each timestamp
        val calendar = Calendar.getInstance(Locale.getDefault())

        // Get the year and day of the photo
        calendar.timeInMillis = photoStartOfDay
        val photoDay = calendar[Calendar.DAY_OF_YEAR]
        val photoYear = calendar[Calendar.YEAR]

        // Get the year and day for today
        calendar.timeInMillis = nowStartOfDay
        val today = calendar[Calendar.DAY_OF_YEAR]
        val todayYear = calendar[Calendar.YEAR]

        when {
            // When the year is the same
            todayYear == photoYear -> {
                return (today - photoDay).toLong()
            }
            // When today's year is past the photo's year
            todayYear > photoYear -> {
                calendar.timeInMillis = photoStartOfDay
                val maxDaysInPhotoYear = calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
                val daysInPhotoYear = maxDaysInPhotoYear - photoDay
                var totalDays = daysInPhotoYear + today

                // Make sure to count years between photo year and this year
                // Example 2013 to 2016 count additional total days in 2014 and 2015
                for (i in (photoYear + 1) until todayYear) {
                    calendar[Calendar.YEAR] = i
                    totalDays += calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
                }

                return totalDays.toLong()
            }
            // When the today's year is before the photo
            // Note: this error case should not be used since this case is checked for on entry to function
            else -> {
                return -1
            }
        }
    }

    // Returns a timestamp at the start of a day
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

    // Returns the number of days until a project is due
    // Number can be negative indicating how for past due a photo is
    fun daysUntilDue(projectView: ProjectView): Long {
        val daysSinceLastPhotoTaken = getDaysSinceTimeStamp(projectView.cover_photo_timestamp, System.currentTimeMillis())
        val interval: Int = projectView.interval_days
        return interval - daysSinceLastPhotoTaken
    }

}