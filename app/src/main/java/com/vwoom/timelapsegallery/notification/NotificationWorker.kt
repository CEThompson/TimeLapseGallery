package com.vwoom.timelapsegallery.notification

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.di.InjectorUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils
import timber.log.Timber

class NotificationWorker(context: Context, params: WorkerParameters)
    : Worker(context, params) {

    private lateinit var projectRepository: ProjectRepository

    override fun doWork(): Result {
        Timber.d("Notification Tracker: Executing work")

        val notificationAlarm = NotificationAlarm()
        notificationAlarm.cancelAlarms(applicationContext)

        // Get all the scheduled projects from the repo
        projectRepository = InjectorUtils.getProjectRepository(applicationContext)
        val scheduledProjects = projectRepository.getScheduledProjectViews()
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val notificationsEnabled = prefs.getBoolean(applicationContext.getString(R.string.key_notifications_enabled), true)
        Timber.d("Notification Tracker: notificationsEnabled == $notificationsEnabled")

        // Cancel if notifications disabled
        if (!notificationsEnabled) {
            NotificationUtils.cancelNotificationWorker(applicationContext)
            return Result.success()
        }

        // If there are projects due today or tomorrow schedule the alarm
        // Note: when the alarm fires should check if projects have been taken care of before sending notification
        val dueProjects = scheduledProjects.filter {
            ProjectUtils.isProjectDueToday(it) || ProjectUtils.isProjectDueTomorrow(it)
        }

        // If there are no projects due today or tomorrow cancel the worker
        if (dueProjects.isEmpty()) {
            Timber.d("Notification Tracker: No projects scheduled")
            NotificationUtils.cancelNotificationWorker(applicationContext)
        }
        // Otherwise schedule the alarm to check for due projects tomorrow
        else {
            Timber.d("Notification Tracker: Projects scheduled")
            notificationAlarm.setAlarmForTomorrow(applicationContext)
        }
        return Result.success()
    }

    companion object {
        const val TAG = "notification_worker"
    }
}