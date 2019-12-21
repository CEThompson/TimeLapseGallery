package com.vwoom.timelapsegallery.widget;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.vwoom.timelapsegallery.data.AppExecutors;
import com.vwoom.timelapsegallery.data.entry.ProjectEntry;
import com.vwoom.timelapsegallery.data.TimeLapseDatabase;

import java.util.List;

public class UpdateWidgetService extends IntentService {

    public static final String ACTION_UPDATE_WIDGETS = "com.example.android.timelapsegallery.UPDATE_WIDGETS";

    public UpdateWidgetService() {
        super("UpdateWidgetService");
    }

    private static final String TAG = UpdateWidgetService.class.getSimpleName();

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null){
            final String action = intent.getAction();
            if (ACTION_UPDATE_WIDGETS.equals(action)) {
                updateWidgets();
            }
        }
    }

    public static void startActionUpdateWidgets(Context context){
        Log.d(TAG, "starting service to update widgets");
        Intent intent = new Intent(context, UpdateWidgetService.class);
        intent.setAction(ACTION_UPDATE_WIDGETS);
        context.startService(intent);
    }

    private void updateWidgets(){
        AppExecutors.getInstance().diskIO().execute(()->{
            // Retrieve the database
            TimeLapseDatabase timeLapseDatabase = TimeLapseDatabase.Companion.getInstance(this);

            // Get the list of all scheduled projects from the database
            List<ProjectEntry> allScheduledProjects = timeLapseDatabase.projectDao().loadAllScheduledProjects();

            // Get the list of projects scheduled for today
            List<ProjectEntry> projectsScheduledForToday = timeLapseDatabase.projectDao().loadAllScheduledProjects();

            // Update the widgets
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, WidgetProvider.class));
            WidgetProvider.updateWidgets(
                    this,
                    appWidgetManager,
                    appWidgetIds,
                    projectsScheduledForToday); // Send the list of todays projects
        });
    }

}
