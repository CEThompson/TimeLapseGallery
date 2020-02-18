package com.vwoom.timelapsegallery.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.vwoom.timelapsegallery.BuildConfig
import com.vwoom.timelapsegallery.notification.NotificationUtils.clearPreviousNotifications
import com.vwoom.timelapsegallery.notification.NotificationUtils.convertDayOfYearToNotificationTime
import com.vwoom.timelapsegallery.notification.NotificationUtils.notifyUserOfScheduledProjects
import java.util.*

class NotificationAlarm : BroadcastReceiver() {
    /* This receives the pending intent broadcast and creates the notification */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Notification Tracker: Receiving intent to create notification")
        // Set up the wake lock
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val w1 = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
        val timeout: Long = 1000 // release wakelock after 1 second
        w1.acquire(timeout)
        // Get the request code
        val requestCode = intent.getIntExtra(REQUEST_CODE, 0)
        Log.d(TAG, "Notification Tracker: Request code is $requestCode")
        Log.d(TAG, "Notification Tracker: Action type is " + intent.action)
        // Clear any previous notifications that are unused
        clearPreviousNotifications(context)
        // Send the notification
        notifyUserOfScheduledProjects(context, requestCode)
        // Release the wake lock
        w1.release()
    }

    /* Set an alarm for tomorrow */
    fun setAlarmForTomorrow(context: Context) {
        Log.d(TAG, "Notification Tracker: Setting alarm for tomorrow")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        val today = calendar[Calendar.DAY_OF_YEAR]
        val requestCode = today + 1
        Log.d(TAG, "Notification Tracker: Request code / day of year is $requestCode")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Create the intent
        val intent = Intent(context, NotificationAlarm::class.java)
        intent.putExtra(REQUEST_CODE, requestCode)
        intent.action = CREATE_NOTIFICATION_AUTHORITY + requestCode
        val pendingIntent = PendingIntent
                .getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        // Convert tomorrow into a timestamp at a set notification time
        val timestampForTomorrow = convertDayOfYearToNotificationTime(requestCode, context)
        alarmManager[AlarmManager.RTC_WAKEUP, timestampForTomorrow] = pendingIntent
    }

    /* Cancels an alarm by the day of year */
    private fun cancelAlarmByDay(context: Context, dayOfYear: Int) {
        Log.d(TAG, "Notification Tracker: Canceling alarm for day $dayOfYear")
        // Set up the alarm manager
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Create the intent
        val intent = Intent(context, NotificationAlarm::class.java)
        // Note: intent action is modified by project id same as above
        intent.action = CREATE_NOTIFICATION_AUTHORITY + dayOfYear
        // Create the pending intent identified by both request code and intent
        val sender = PendingIntent.getBroadcast(context, dayOfYear, intent, 0)
        // Now cancel the alarm
        alarmManager.cancel(sender)
    }

    /* Cancels any alarms set for today or tomorrow */
    fun cancelAlarms(context: Context) {
        Log.d(TAG, "Notification Tracker: Canceling alarms for today and tomorrow")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        val today = calendar[Calendar.DAY_OF_YEAR]
        val tomorrow = today + 1
        cancelAlarmByDay(context, today)
        cancelAlarmByDay(context, tomorrow)
    }

    companion object {
        private val TAG = NotificationAlarm::class.java.simpleName
        private const val REQUEST_CODE = "request_code"
        private const val applicationId = BuildConfig.APPLICATION_ID
        private const val CREATE_NOTIFICATION_AUTHORITY = "$applicationId.CREATE_NOTIFICATION"
    }
}