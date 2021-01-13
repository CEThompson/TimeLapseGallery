package com.vwoom.timelapsegallery.widget

import android.app.IntentService
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.di.InjectorUtils
import com.vwoom.timelapsegallery.widget.WidgetProvider.Companion.updateWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class UpdateWidgetService
    : IntentService("UpdateWidgetService"), CoroutineScope {

    private lateinit var projectRepository: ProjectRepository

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
        launch {
            // Get the list of all scheduled projects from the database
            projectRepository = InjectorUtils.getProjectRepository(applicationContext)
            val allScheduledProjects = projectRepository.getScheduledProjectViews()

            // Update the widgets
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            val appWidgetIds: IntArray = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, WidgetProvider::class.java))
            updateWidgets(
                    applicationContext,
                    appWidgetManager,
                    appWidgetIds,
                    allScheduledProjects) // Send the list of all scheduled projects
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGETS = "com.example.android.timelapsegallery.UPDATE_WIDGETS"
        fun startActionUpdateWidgets(context: Context) {
            Timber.d("WidgetTracker: starting service to update widgets")
            val intent = Intent(context, UpdateWidgetService::class.java)
            intent.action = ACTION_UPDATE_WIDGETS
            context.startService(intent)
        }
    }
}