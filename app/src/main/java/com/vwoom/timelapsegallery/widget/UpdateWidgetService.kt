package com.vwoom.timelapsegallery.widget

import android.app.IntentService
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vwoom.timelapsegallery.data.AppExecutors.Companion.instance
import com.vwoom.timelapsegallery.data.TimeLapseDatabase.Companion.getInstance
import com.vwoom.timelapsegallery.widget.WidgetProvider
import com.vwoom.timelapsegallery.widget.WidgetProvider.Companion.updateWidgets

class UpdateWidgetService : IntentService("UpdateWidgetService") {
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            if (ACTION_UPDATE_WIDGETS == action) {
                updateWidgets()
            }
        }
    }

    private fun updateWidgets() {
        instance!!.diskIO().execute {
            // Retrieve the database
            val timeLapseDatabase = getInstance(this)
            // Get the list of all scheduled projects from the database
            val allScheduledProjects = timeLapseDatabase.projectDao().loadAllScheduledProjects()
            // Get the list of projects scheduled for today
            val projectsScheduledForToday = timeLapseDatabase.projectDao().loadAllScheduledProjects()
            // Update the widgets
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, WidgetProvider::class.java))
            updateWidgets(
                    this,
                    appWidgetManager,
                    appWidgetIds,
                    projectsScheduledForToday) // Send the list of todays projects
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGETS = "com.example.android.timelapsegallery.UPDATE_WIDGETS"
        private val TAG = UpdateWidgetService::class.java.simpleName
        fun startActionUpdateWidgets(context: Context) {
            Log.d(TAG, "starting service to update widgets")
            val intent = Intent(context, UpdateWidgetService::class.java)
            intent.action = ACTION_UPDATE_WIDGETS
            context.startService(intent)
        }
    }
}