package com.vwoom.timelapsegallery.widget

import android.app.IntentService
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vwoom.timelapsegallery.data.TimeLapseDatabase.Companion.getInstance
import com.vwoom.timelapsegallery.widget.WidgetProvider.Companion.updateWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class UpdateWidgetService : IntentService("UpdateWidgetService"), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            if (ACTION_UPDATE_WIDGETS == action) {
                updateWidgets()
            }
        }
    }

    private fun updateWidgets() {
        val context = this
        launch {
                // Retrieve the database
                val timeLapseDatabase = getInstance(context)
                // Get the list of all scheduled projects from the database
                val allScheduledProjects = timeLapseDatabase.projectDao().loadAllScheduledProjects()
                // Get the list of projects scheduled for today
                val projectsScheduledForToday = timeLapseDatabase.projectDao().loadAllScheduledProjects()
                // Update the widgets
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java))
                updateWidgets(
                        context,
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