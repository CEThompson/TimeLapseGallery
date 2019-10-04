package com.vwoom.timelapsegallery.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.vwoom.timelapsegallery.R;
import com.vwoom.timelapsegallery.activities.MainActivity;
import com.vwoom.timelapsegallery.utils.Keys;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public final class NotificationUtils {

    private static final String PROJECT_NOTIFICATION_CHANNEL_ID = "project_channel_id";

    private static final String CHANNEL_NAME = "channel_name";

    private static final String CHANNEL_DESCRIPTION = "channel_description";

    private static final String TAG = NotificationUtils.class.getSimpleName();

    /* Creates a notification that launches the main activity and filters by todays scheduled projects */
    public static void notifyUserOfScheduledProjects(Context context, int requestCode){
        Log.d(TAG, "Notification Tracker: Notifying user of scheduled projects for request code " + requestCode);

        String title = context.getString(R.string.app_name);
        String content = context.getString(R.string.projects_scheduled_today);

        // Create the pending intent to take the user to the relevant project on click
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Keys.PROJECT_FILTER_BY_SCHEDULED_TODAY, true);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode, intent, 0);

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, PROJECT_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_add_a_photo_white_24dp)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_SOUND)
                .setAutoCancel(true);

        // Create the channel for android 8.0+
        createNotificationChannel(context);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // use request code as notification ID
        notificationManager.notify(requestCode, builder.build());
    }


    /* Creates the notification channel for android 8.0 + */
    private static void createNotificationChannel(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(
                    PROJECT_NOTIFICATION_CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESCRIPTION);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /* Returns a timestamp for a notification by day of the year*/
    public static long convertDayOfYearToNotificationTime(int dayOfYear, Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String notificationTimeString = prefs.getString(context.getString(R.string.key_notification_time), "7");
        int notificationTime = Integer.parseInt(notificationTimeString);
        Log.d(TAG, "Notification Tracker: shared pref time = " + notificationTimeString + " int is " + notificationTime);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_YEAR, dayOfYear);
        c.set(Calendar.HOUR_OF_DAY, notificationTime);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        Log.d(TAG, "Notification Tracker: returning notification time in ms = " + c.getTimeInMillis());
        return c.getTimeInMillis();
    }

    /* Cancels any previous notification workers and schedules a new periodic notification worker.
    * The notification workers checks each day to set a notification for tomorrow. */
    public static void scheduleNotificationWorker(Context context){
        WorkManager.getInstance(context).cancelAllWorkByTag(NotificationWorker.TAG);
        Log.d(TAG, "Notification Tracker: Creating and enqueuing work reqeust");
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest scheduleNotificationsRequest =
                new PeriodicWorkRequest.Builder(NotificationWorker.class, 1, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        .addTag(NotificationWorker.TAG)
                        .build();

        WorkManager.getInstance(context).enqueue(scheduleNotificationsRequest);
    }

    /* Cancels alarms AND cancels any notification workers*/
    public static void cancelNotificationWorker(Context context){
        Log.d(TAG, "Notification Tracker: Canceling notifications");
        NotificationAlarm notificationAlarm = new NotificationAlarm();
        notificationAlarm.cancelAlarms(context);
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG);
    }

}
