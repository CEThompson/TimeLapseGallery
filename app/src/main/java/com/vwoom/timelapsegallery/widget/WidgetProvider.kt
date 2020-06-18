package com.vwoom.timelapsegallery.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import androidx.navigation.NavDeepLinkBuilder
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.utils.ProjectUtils

// TODO (deferred): showcase a random project per day
class WidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        UpdateWidgetService.startActionUpdateWidgets(context)
    }

    companion object {
        val TAG = WidgetProvider::class.java.simpleName

        @JvmStatic
        fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, projectViews: List<ProjectView>) {
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, projectViews)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, projectViews: List<ProjectView>) {
            val projectsDueToday = projectViews.filter { ProjectUtils.isProjectDueToday(it) }

            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setTextViewText(R.id.widget_text_view, context.getString(R.string.x_projects_due_today, projectsDueToday.size))

            val bundle = Bundle()

            // If there are projects due today, put the search key for due projects as true
            // Otherwise normal start up
            if (projectsDueToday.isNotEmpty()) { bundle.putBoolean(context.getString(R.string.search_launch_due), true) }

            val pendingIntent = NavDeepLinkBuilder(context)
                    .setComponentName(TimeLapseGalleryActivity::class.java)
                    .setGraph(R.navigation.nav_graph)
                    .setDestination(R.id.galleryFragment)
                    .setArguments(bundle)
                    .createPendingIntent()
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}