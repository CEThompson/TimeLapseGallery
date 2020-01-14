package com.vwoom.timelapsegallery.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.entry.ProjectEntry

class WidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        UpdateWidgetService.startActionUpdateWidgets(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }

    companion object {
        @JvmStatic
        fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, projects: List<ProjectEntry>) {
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, projects)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, projects: List<ProjectEntry>) {
            val views: RemoteViews
            // If there are no projects handle the layout here
            if (projects.size == 0) {
                views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setTextViewText(R.id.widget_text_view, context.getString(R.string.no_projects_for_today))
                views.setViewVisibility(R.id.widget_text_view, View.VISIBLE)
                views.setViewVisibility(R.id.widget_list_view, View.INVISIBLE)
            } else {
                views = getGridRemoteViews(context)
                views.setViewVisibility(R.id.widget_text_view, View.INVISIBLE)
                views.setViewVisibility(R.id.widget_list_view, View.VISIBLE)
            }
            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /* Creates the grid of projects for the widget */
        private fun getGridRemoteViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val adapterIntent = Intent(context, WidgetGridRemoteViewsService::class.java)
            views.setRemoteAdapter(R.id.widget_list_view, adapterIntent)
            // TODO navigate to detail fragment
            val appIntent = Intent(context, TimeLapseGalleryActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            views.setPendingIntentTemplate(R.id.widget_list_view, appPendingIntent)
            return views
        }
    }
}