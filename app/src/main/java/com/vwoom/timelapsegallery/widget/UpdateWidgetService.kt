package com.vwoom.timelapsegallery.widget

import android.app.IntentService
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vwoom.timelapsegallery.utils.InjectorUtils
import com.vwoom.timelapsegallery.widget.WidgetProvider.Companion.updateWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class UpdateWidgetService
    : IntentService("UpdateWidgetService"), CoroutineScope {

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
        val context = applicationContext
        launch {
            // Retrieve the database
            val projectRepository = InjectorUtils.getProjectRepository(context)
            // Get the list of all scheduled projects from the database
            val allScheduledProjects = projectRepository.getScheduledProjects()

            // Update the widgets
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds: IntArray = appWidgetManager.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java))
            updateWidgets(
                    context,
                    appWidgetManager,
                    appWidgetIds,
                    allScheduledProjects) // Send the list of projects scheduled for today

        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGETS = "com.example.android.timelapsegallery.UPDATE_WIDGETS"
        private val TAG = UpdateWidgetService::class.java.simpleName
        fun startActionUpdateWidgets(context: Context) {
            Log.d(TAG, "WidgetTracker: starting service to update widgets")
            val intent = Intent(context, UpdateWidgetService::class.java)
            intent.action = ACTION_UPDATE_WIDGETS
            context.startService(intent)
        }
    }
}