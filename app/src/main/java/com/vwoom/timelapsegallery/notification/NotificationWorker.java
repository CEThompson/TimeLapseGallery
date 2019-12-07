package com.vwoom.timelapsegallery.notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.vwoom.timelapsegallery.R;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectScheduleEntry;
import com.vwoom.timelapsegallery.utils.TimeUtils;

import java.util.Calendar;
import java.util.List;

public class NotificationWorker extends Worker {

    public static final String TAG = "notification_worker";

    public NotificationWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params){
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Notification Tracker: Executing work");
        // Get all the scheduled projects from the database
        TimeLapseDatabase timeLapseDatabase = TimeLapseDatabase.getInstance(getApplicationContext());
        List<ProjectEntry> scheduledProjects = timeLapseDatabase.projectDao().loadAllScheduledProjects();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean notificationsEnabled = prefs.getBoolean(getApplicationContext().getString(R.string.key_notifications_enabled), true);
        Log.d(TAG, "Notification Tracker: notificationsEnabled == " + notificationsEnabled);

        // If there are no scheduled projects cancel the worker
        if (scheduledProjects.size()==0){
            Log.d(TAG, "Notification Tracker: No projects scheduled");
            NotificationUtils.cancelNotificationWorker(getApplicationContext());
        }
        // If notifications are disabled do the same
        else if(!notificationsEnabled){
            Log.d(TAG, "Notification Tracker: Notifications are disabled");
            NotificationUtils.cancelNotificationWorker(getApplicationContext());
        }
        // Otherwise schedule the notifications
        else {
            Log.d(TAG, "Notification Tracker: Projects scheduled");

            boolean scheduleNotification = false;

            // Loop through scheduled projects and set up alarms for each day
            for (ProjectEntry scheduledProject: scheduledProjects){
                ProjectScheduleEntry schedule = timeLapseDatabase
                        .projectScheduleDao()
                        .loadScheduleByProjectId(scheduledProject.getId());

                Log.d(TAG, "Notification Tracker: Processing alarm for project named " + scheduledProject.getProject_name());

                if (schedule == null) break;

                long nextSubmissionTime = schedule.getSchedule_time();
                long dayInterval = schedule.getInterval_days();

                // Schedule a notification for tomorrow
                // If any project has a daily schedule create the notification
                if (dayInterval == 1) {
                    scheduleNotification = true;
                    break;
                }
                // If a project has a weekly alarm check if the alarm is set for tomorrow
                else {
                    // If a weekly project is scheduled for tomorrow set the notification flag and stop
                    if (TimeUtils.isTomorrow(nextSubmissionTime)){
                        scheduleNotification = true;
                        break;
                    }
                    // If current time is past the submission time, and the date is not today then time belongs to yesterday
                    // Or a day beyond yesterday, in this case remind the user that a project submission has elapsed
                    if (System.currentTimeMillis() > nextSubmissionTime && !DateUtils.isToday(nextSubmissionTime)) {
                        scheduleNotification = true;
                        break;
                    }
                }
            }

            // Schedule the notification for tomorrow if one is scheduled
            NotificationAlarm notificationAlarm = new NotificationAlarm();
            if (scheduleNotification) {
                Log.d(TAG, "Notification Tracker: scheduling notification for tomorrow");
                notificationAlarm.setAlarmForTomorrow(getApplicationContext());
            }
            // Otherwise ensure any previously scheduled notifications are canceled
            else {
                Log.d(TAG, "Notification Tracker: no notifications for tomorrow");
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                int tomorrow = calendar.get(Calendar.DAY_OF_YEAR) + 1;
                notificationAlarm.cancelAlarmByDay(getApplicationContext(), tomorrow);
            }
        }

        return Result.success();
    }
}
