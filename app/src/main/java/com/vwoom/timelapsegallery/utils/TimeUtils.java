package com.vwoom.timelapsegallery.utils;

import android.content.Context;
import android.text.format.DateUtils;

import com.vwoom.timelapsegallery.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {
    public static long DAY_IN_MILLISECONDS = (1000 * 60 * 60 * 24);
    public static long WEEK_IN_MILLISECONDS = (1000 * 60 * 60 * 24 * 7);

    public static int SCHEDULE_NONE = 0;
    public static int SCHEDULE_DAILY = 1;
    public static int SCHEDULE_WEEKLY = 2;

    /* Creates a time interval from a schedule identification */
    public static long getTimeIntervalFromSchedule(int schedule){
        long alarmInterval = 0;

        // Minutes = 1000 ms * 60 seconds per minute * 60 minutes per hour * 24 hours per day
        if (schedule == SCHEDULE_DAILY){
            alarmInterval = DAY_IN_MILLISECONDS; // milliseconds * seconds * minutes
        }
        // Etc.
        else if (schedule == SCHEDULE_WEEKLY){
            alarmInterval = WEEK_IN_MILLISECONDS;
        }

        return alarmInterval;
    }

    /* Derives the next scheduled submission from a project timestamp */
    public static long getNextScheduledSubmission(long timestamp, int schedule){
        long scheduleInterval = getTimeIntervalFromSchedule(schedule);

        // prevent an infinite loop, if returned timestamp is less than system time a notification will ping immediately
        if (scheduleInterval < DAY_IN_MILLISECONDS) return timestamp;

        while (timestamp < System.currentTimeMillis()) timestamp += scheduleInterval;
        if (DateUtils.isToday(timestamp)) timestamp += scheduleInterval;
        return timestamp;
    }

    /* Gets the schedule int identification from the selected spinner item */
    public static int convertScheduleStringToInt(Context context, String scheduleString){
        int interval = SCHEDULE_NONE;
        if (scheduleString.equals(context.getString(R.string.daily))) interval = SCHEDULE_DAILY;
        else if (scheduleString.equals(context.getString(R.string.weekly))) interval = SCHEDULE_WEEKLY;
        return interval;
    }

    public static int getHourFromTimestamp(long timestamp){
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        return c.get(Calendar.HOUR_OF_DAY);
    }

    public static int getMinutesFromTimestamp(long timestamp){
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        return c.get(Calendar.MINUTE);
    }

    public static String getDateFromTimestamp(long timestamp){
        return new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(new Date(timestamp));
    }

    public static String getTimeFromTimestamp(long timestamp){
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(timestamp));
    }

    public static String getDayFromTimestamp(long timestamp){
        return new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date(timestamp));
    }

    public static boolean isTomorrow(long timestamp){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int today = calendar.get(Calendar.DAY_OF_YEAR);
        int tomorrow = today + 1;

        calendar.setTimeInMillis(timestamp);
        int dayOfTimestamp = calendar.get(Calendar.DAY_OF_YEAR);

        return (dayOfTimestamp == tomorrow);
    }
}
