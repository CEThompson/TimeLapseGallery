package com.vwoom.timelapsecollection.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.vwoom.timelapsecollection.utils.Keys;

import java.util.Calendar;

public class NotificationAlarm extends BroadcastReceiver {

    private static final String TAG = NotificationAlarm.class.getSimpleName();

    /* This receives the pending intent broadcast and creates the notification */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Notification Tracker: Receiving intent to create notification");
        // Set up the wake lock
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock w1 = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        long timeout = 1000;    // release wakelock after 1 second
        w1.acquire(timeout);

        // Get the request code
        int requestCode = intent.getIntExtra(Keys.REQUEST_CODE, 0);

        Log.d(TAG, "Notification Tracker: Request code is " + requestCode);
        Log.d(TAG, "Notification Tracker: Action type is " + intent.getAction());

        // Send the notification
        NotificationUtils.notifyUserOfScheduledProjects(context, requestCode);

        // Release the wake lock
        w1.release();
    }

    /* Set an alarm for tomorrow */
    public void setAlarmForTomorrow(Context context){
        Log.d(TAG, "Notification Tracker: Setting alarm for tomorrow");

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        int today = calendar.get(Calendar.DAY_OF_YEAR);
        int requestCode = today + 1;

        Log.d(TAG, "Notification Tracker: Request code / day of year is " + requestCode);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        // Create the intent
        Intent intent = new Intent(context, NotificationAlarm.class);
        intent.putExtra(Keys.REQUEST_CODE, requestCode);

        intent.setAction(Keys.CREATE_NOTIFICATION_AUTHORITY + requestCode);

        PendingIntent pendingIntent = PendingIntent
                .getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Convert tomorrow into a timestamp at a set notification time
        long timestampForTomorrow = NotificationUtils.convertDayOfYearToNotificationTime(requestCode);

        alarmManager.set(AlarmManager.RTC_WAKEUP, timestampForTomorrow, pendingIntent);
    }

    /* Cancels an alarm by the day of year */
    public void cancelAlarmByDay(Context context, int dayOfYear){
        Log.d(TAG, "Notification Tracker: Canceling alarm for day " + dayOfYear);
        // Set up the alarm manager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Create the intent
        Intent intent = new Intent(context, NotificationAlarm.class);
        // Note: intent action is modified by project id same as above
        intent.setAction(Keys.CREATE_NOTIFICATION_AUTHORITY + dayOfYear);

        // Create the pending intent identified by both request code and intent
        PendingIntent sender = PendingIntent.getBroadcast(context, dayOfYear, intent, 0);

        // Now cancel the alarm
        alarmManager.cancel(sender);
    }

    /* Cancels any alarms set for today or tomorrow */
    public void cancelAlarms(Context context){
        Log.d(TAG, "Notification Tracker: Canceling alarms for today and tomorrow");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        int today = calendar.get(Calendar.DAY_OF_YEAR);
        int tomorrow = today + 1;

        cancelAlarmByDay(context, today);
        cancelAlarmByDay(context, tomorrow);
    }
}
