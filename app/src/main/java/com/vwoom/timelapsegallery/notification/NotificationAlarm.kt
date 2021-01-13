package com.vwoom.timelapsegallery.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.vwoom.timelapsegallery.BuildConfig
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.notification.NotificationUtils.clearPreviousNotifications
import com.vwoom.timelapsegallery.notification.NotificationUtils.convertDayOfYearToNotificationTime
import com.vwoom.timelapsegallery.notification.NotificationUtils.notifyUserOfScheduledProjects
import com.vwoom.timelapsegallery.di.InjectorUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class NotificationAlarm : BroadcastReceiver() {

    private lateinit var projectRepository: ProjectRepository

    // This receives the pending intent broadcast from the notification alarm and creates a notification
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Notification Tracker: Receiving intent to create notification")

        // Set up the wake lock
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val w1 = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
        val timeout: Long = 1000 // release wakelock after 1 second
        w1.acquire(timeout)

        // Get the request code which is the day of the year
        val requestCode = intent.getIntExtra(REQUEST_CODE, 0)
        Timber.d("Notification Tracker: Request code is $requestCode")
        Timber.d("Notification Tracker: Action type is ${intent.action}")
        // Clear any previous notifications that are unused
        clearPreviousNotifications(context)

        GlobalScope.launch(Dispatchers.IO) {
            // Get the scheduled projects and figure out if any projects are due today
            projectRepository = InjectorUtils.getProjectRepository(context)
            val scheduledProjects = projectRepository.getScheduledProjectViews()
            val dueProjects = scheduledProjects.filter {
                ProjectUtils.isProjectDueToday(it)
            }

            Timber.d("there are ${dueProjects.size} projects due")
            Timber.d("due projects is not empty ${dueProjects.isNotEmpty()}")
            // If there are projects due today, send the notification
            if (dueProjects.isNotEmpty()) notifyUserOfScheduledProjects(context, requestCode)
            // Release the wake lock
            w1.release()
        }
    }

    // Sets an alarm for tomorrow at the shared preference time
    fun setAlarmForTomorrow(context: Context) {
        Timber.d("Notification Tracker: Setting alarm for tomorrow")
        // Get the day of the year and set it to the request code
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        val today = calendar[Calendar.DAY_OF_YEAR]
        val requestCode = today + 1

        // Create the pending intent to broadcast on that day
        Timber.d("Notification Tracker: Request code / day of year is $requestCode")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationAlarm::class.java)
        intent.putExtra(REQUEST_CODE, requestCode)
        intent.action = CREATE_NOTIFICATION_AUTHORITY + requestCode
        val pendingIntent = PendingIntent
                .getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Get the timestamp from the shared preference for the day at that notification time
        val timestampForTomorrow = convertDayOfYearToNotificationTime(requestCode, context)

        // Set the alarm
        alarmManager[AlarmManager.RTC_WAKEUP, timestampForTomorrow] = pendingIntent
    }

    // Helper function that Cancels an alarm by the day of year
    private fun cancelAlarmByDay(context: Context, dayOfYear: Int) {
        Timber.d("Notification Tracker: Canceling alarm for day $dayOfYear")

        // Identify the alarm
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationAlarm::class.java)
        intent.action = CREATE_NOTIFICATION_AUTHORITY + dayOfYear
        // Create the pending intent to remove the alarm by day
        val sender = PendingIntent.getBroadcast(context, dayOfYear, intent, 0)

        // Now cancel the alarm
        alarmManager.cancel(sender)
    }

    // Cancels any alarms set for today or tomorrow
    // This should be any alarms set
    fun cancelAlarms(context: Context) {
        Timber.d("Notification Tracker: Canceling alarms for today and tomorrow")
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