package com.vwoom.timelapsegallery.notification

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.repository.ProjectScheduleRepository
import com.vwoom.timelapsegallery.utils.InjectorUtils
import com.vwoom.timelapsegallery.utils.TimeUtils.isTomorrow
import java.util.*

class NotificationWorker(context: Context, params: WorkerParameters)
    : Worker(context, params)
{
    lateinit var projectRepository: ProjectRepository

    override fun doWork(): Result {
        Log.d(TAG, "Notification Tracker: Executing work")

        projectRepository = InjectorUtils.getProjectRepository(applicationContext)

        val notificationAlarm = NotificationAlarm()
        notificationAlarm.cancelAlarms(applicationContext)

        // Get all the scheduled projects from the repo
        val scheduledProjects = projectRepository.getScheduledProjectsNonSuspend()
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val notificationsEnabled = prefs.getBoolean(applicationContext.getString(R.string.key_notifications_enabled), true)
        Log.d(TAG, "Notification Tracker: notificationsEnabled == $notificationsEnabled")

        // If there are no scheduled projects cancel the worker
        if (scheduledProjects.isEmpty()) {
            Log.d(TAG, "Notification Tracker: No projects scheduled")
            NotificationUtils.cancelNotificationWorker(applicationContext)
        }
        // Also cancel the worker if notifications have been disabled
        else if (!notificationsEnabled) {
            Log.d(TAG, "Notification Tracker: Notifications are disabled")
            NotificationUtils.cancelNotificationWorker(applicationContext)
        }
        // Schedule an alarm for tomorrow if there are any scheduled projects
        else {
            Log.d(TAG, "Notification Tracker: Projects scheduled")
            notificationAlarm.setAlarmForTomorrow(applicationContext)
        }
        return Result.success()
    }

    companion object {
        const val TAG = "notification_worker"
    }
}