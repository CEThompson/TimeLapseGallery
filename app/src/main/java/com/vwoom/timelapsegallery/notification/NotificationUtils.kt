package com.vwoom.timelapsegallery.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.notification.NotificationWorker
import com.vwoom.timelapsegallery.utils.Keys
import java.util.*
import java.util.concurrent.TimeUnit

object NotificationUtils {
    private const val PROJECT_NOTIFICATION_CHANNEL_ID = "project_channel_id"
    private const val CHANNEL_NAME = "channel_name"
    private const val CHANNEL_DESCRIPTION = "channel_description"
    private val TAG = NotificationUtils::class.java.simpleName
    /* Creates a notification that launches the main activity and filters by todays scheduled projects */
    @JvmStatic
    fun notifyUserOfScheduledProjects(context: Context, requestCode: Int) {
        Log.d(TAG, "Notification Tracker: Notifying user of scheduled projects for request code $requestCode")
        val title = context.getString(R.string.app_name)
        val content = context.getString(R.string.projects_scheduled_today)
        // Create the pending intent to take the user to the relevant project on click
        val intent = Intent(context, TimeLapseGalleryActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra(Keys.PROJECT_FILTER_BY_SCHEDULED_TODAY, true)
        val pendingIntent = PendingIntent.getActivity(context, requestCode, intent, 0)
        // Create the notification
        val builder = NotificationCompat.Builder(context, PROJECT_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_add_a_photo_white_24dp)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_SOUND)
                .setAutoCancel(true)
        // Create the channel for android 8.0+
        createNotificationChannel(context)
        // Show the notification
        val notificationManager = NotificationManagerCompat.from(context)
        // use request code as notification ID
        notificationManager.notify(requestCode, builder.build())
    }

    /* Clears any unused notifications */
    @JvmStatic
    fun clearPreviousNotifications(context: Context?) {
        val notificationManager = NotificationManagerCompat.from(context!!)
        notificationManager.cancelAll()
    }

    /* Creates the notification channel for android 8.0 + */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    PROJECT_NOTIFICATION_CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = CHANNEL_DESCRIPTION
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /* Returns a timestamp for a notification by day of the year*/
    @JvmStatic
    fun convertDayOfYearToNotificationTime(dayOfYear: Int, context: Context): Long {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val notificationTimeString = prefs.getString(context.getString(R.string.key_notification_time), "7")
        val notificationTime = notificationTimeString!!.toInt()
        Log.d(TAG, "Notification Tracker: shared pref time = $notificationTimeString int is $notificationTime")
        val c = Calendar.getInstance()
        c[Calendar.DAY_OF_YEAR] = dayOfYear
        c[Calendar.HOUR_OF_DAY] = notificationTime
        c[Calendar.MINUTE] = 0
        c[Calendar.SECOND] = 0
        Log.d(TAG, "Notification Tracker: returning notification time in ms = " + c.timeInMillis)
        return c.timeInMillis
    }

    /* Cancels any previous notification workers and schedules a new periodic notification worker.
    * The notification workers checks each day to set a notification for tomorrow. */
    fun scheduleNotificationWorker(context: Context) {
        WorkManager.getInstance(context!!).cancelAllWorkByTag(NotificationWorker.TAG)
        Log.d(TAG, "Notification Tracker: Creating and enqueuing work reqeust")
        val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        val scheduleNotificationsRequest = PeriodicWorkRequest.Builder(NotificationWorker::class.java, 1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .addTag(NotificationWorker.TAG)
                .build()
        WorkManager.getInstance(context).enqueue(scheduleNotificationsRequest)
    }

    /* Cancels alarms AND cancels any notification workers*/
    fun cancelNotificationWorker(context: Context) {
        Log.d(TAG, "Notification Tracker: Canceling notifications")
        val notificationAlarm = NotificationAlarm()
        notificationAlarm.cancelAlarms(context)
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }
}