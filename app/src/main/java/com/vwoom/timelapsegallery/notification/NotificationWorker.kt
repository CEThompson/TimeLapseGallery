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
import com.vwoom.timelapsegallery.utils.TimeUtils.isTomorrow
import java.util.*

class NotificationWorker(context: Context, params: WorkerParameters,
        private val projectRepository: ProjectRepository,
        private val projectScheduleRepository: ProjectScheduleRepository)
    : Worker(context, params)
{
    override fun doWork(): Result {
        Log.d(TAG, "Notification Tracker: Executing work")

        // Get all the scheduled projects from the repo
        val scheduledProjects = projectRepository.getScheduledProjects()
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val notificationsEnabled = prefs.getBoolean(applicationContext.getString(R.string.key_notifications_enabled), true)
        Log.d(TAG, "Notification Tracker: notificationsEnabled == $notificationsEnabled")

        // If there are no scheduled projects cancel the worker
        if (scheduledProjects.isEmpty()) {
            Log.d(TAG, "Notification Tracker: No projects scheduled")
            NotificationUtils.cancelNotificationWorker(applicationContext)
        } else if (!notificationsEnabled) {
            Log.d(TAG, "Notification Tracker: Notifications are disabled")
            NotificationUtils.cancelNotificationWorker(applicationContext)
        } else {
            Log.d(TAG, "Notification Tracker: Projects scheduled")
            var scheduleNotification = false
            // Loop through scheduled projects and set up alarms for each day
            for (scheduledProject in scheduledProjects) {
                val schedule = projectScheduleRepository.getProjectSchedule(scheduledProject.id)
                Log.d(TAG, "Notification Tracker: Processing alarm for project named " + scheduledProject.project_name)

                val nextSubmissionTime = schedule.schedule_time!!
                val dayInterval = schedule.interval_days!!.toLong()
                // Schedule a notification for tomorrow
// If any project has a daily schedule create the notification
                if (dayInterval == 1L) {
                    scheduleNotification = true
                    break
                } else { // If a weekly project is scheduled for tomorrow set the notification flag and stop
                    if (isTomorrow(nextSubmissionTime)) {
                        scheduleNotification = true
                        break
                    }
                    // If current time is past the submission time, and the date is not today then time belongs to yesterday
// Or a day beyond yesterday, in this case remind the user that a project submission has elapsed
                    if (System.currentTimeMillis() > nextSubmissionTime && !DateUtils.isToday(nextSubmissionTime)) {
                        scheduleNotification = true
                        break
                    }
                }
            }
            // Schedule the notification for tomorrow if one is scheduled
            val notificationAlarm = NotificationAlarm()
            if (scheduleNotification) {
                Log.d(TAG, "Notification Tracker: scheduling notification for tomorrow")
                notificationAlarm.setAlarmForTomorrow(applicationContext)
            } else {
                Log.d(TAG, "Notification Tracker: no notifications for tomorrow")
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = System.currentTimeMillis()
                val tomorrow = calendar[Calendar.DAY_OF_YEAR] + 1
                notificationAlarm.cancelAlarmByDay(applicationContext, tomorrow)
            }
        }
        return Result.success()
    }

    companion object {
        const val TAG = "notification_worker"
    }
}