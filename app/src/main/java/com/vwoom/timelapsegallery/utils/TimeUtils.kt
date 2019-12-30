package com.vwoom.timelapsegallery.utils

import android.content.Context
import android.text.format.DateUtils
import com.vwoom.timelapsegallery.R
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    var DAY_IN_MILLISECONDS = (1000 * 60 * 60 * 24).toLong()
    var WEEK_IN_MILLISECONDS = (1000 * 60 * 60 * 24 * 7).toLong()
    var SCHEDULE_NONE = 0
    var SCHEDULE_DAILY = 1
    var SCHEDULE_WEEKLY = 2
    /* Creates a time interval from a schedule identification */
    fun getTimeIntervalFromSchedule(schedule: Int): Long {
        var alarmInterval: Long = 0
        // Minutes = 1000 ms * 60 seconds per minute * 60 minutes per hour * 24 hours per day
        if (schedule == SCHEDULE_DAILY) {
            alarmInterval = DAY_IN_MILLISECONDS // milliseconds * seconds * minutes
        } else if (schedule == SCHEDULE_WEEKLY) {
            alarmInterval = WEEK_IN_MILLISECONDS
        }
        return alarmInterval
    }

    /* Derives the next scheduled submission from a project timestamp */
    @JvmStatic
    fun getNextScheduledSubmission(timestamp: Long, schedule: Int): Long {
        var timestamp = timestamp
        val scheduleInterval = getTimeIntervalFromSchedule(schedule)
        // prevent an infinite loop, if returned timestamp is less than system time a notification will ping immediately
        if (scheduleInterval < DAY_IN_MILLISECONDS) return timestamp
        while (timestamp < System.currentTimeMillis()) timestamp += scheduleInterval
        if (DateUtils.isToday(timestamp)) timestamp += scheduleInterval
        return timestamp
    }

    /* Gets the schedule int identification from the selected spinner item */
    fun convertScheduleStringToInt(context: Context, scheduleString: String): Int {
        var interval = SCHEDULE_NONE
        if (scheduleString == context.getString(R.string.daily)) interval = SCHEDULE_DAILY else if (scheduleString == context.getString(R.string.weekly)) interval = SCHEDULE_WEEKLY
        return interval
    }

    fun getHourFromTimestamp(timestamp: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = timestamp
        return c[Calendar.HOUR_OF_DAY]
    }

    fun getMinutesFromTimestamp(timestamp: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = timestamp
        return c[Calendar.MINUTE]
    }

    fun getShortDateFromTimestamp(timestamp: Long): String {
        return SimpleDateFormat("M/d/y", Locale.getDefault()).format(Date(timestamp))
    }

    fun getDateFromTimestamp(timestamp: Long): String {
        return SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    @JvmStatic
    fun getTimeFromTimestamp(timestamp: Long): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    fun getDayFromTimestamp(timestamp: Long): String {
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
    }

    @JvmStatic
    fun isTomorrow(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        val today = calendar[Calendar.DAY_OF_YEAR]
        val tomorrow = today + 1
        calendar.timeInMillis = timestamp
        val dayOfTimestamp = calendar[Calendar.DAY_OF_YEAR]
        return dayOfTimestamp == tomorrow
    }
}