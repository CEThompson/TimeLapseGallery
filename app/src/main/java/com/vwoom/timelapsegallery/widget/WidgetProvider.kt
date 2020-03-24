package com.vwoom.timelapsegallery.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.navigation.NavDeepLinkBuilder
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.entry.ProjectEntry

// TODO: restructure widget to implement branding,
// TODO: showcase a random project per day and provide a quick link to projects due today
class WidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        UpdateWidgetService.startActionUpdateWidgets(context)
    }

    companion object {
        val TAG = WidgetProvider::class.java.simpleName

        @JvmStatic
        fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, projects: List<ProjectEntry>) {
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, projects)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, projects: List<ProjectEntry>) {
            val views: RemoteViews
            // If there are no projects handle the layout here
            if (projects.isEmpty()) {
                views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setTextViewText(R.id.widget_text_view, context.getString(R.string.no_projects_for_today))
                views.setViewVisibility(R.id.widget_text_view, View.VISIBLE)
                views.setViewVisibility(R.id.widget_grid_view, View.INVISIBLE)
            } else {
                views = getGridRemoteViews(context)
                views.setViewVisibility(R.id.widget_text_view, View.INVISIBLE)
                views.setViewVisibility(R.id.widget_grid_view, View.VISIBLE)
            }
            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /* Creates the grid of projects for the widget */
        private fun getGridRemoteViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val adapterIntent = Intent(context, WidgetGridRemoteViewsService::class.java)
            views.setRemoteAdapter(R.id.widget_grid_view, adapterIntent)

            //val appIntent = Intent(context, TimeLapseGalleryActivity::class.java)

            // Create the nav pending intent
            val bundle = Bundle()
            bundle.putBoolean(context.getString(R.string.search_launch_due), true)
            // Create the pending intent to nav to the gallery with the argument
            val pendingIntent = NavDeepLinkBuilder(context)
                    .setComponentName(TimeLapseGalleryActivity::class.java)
                    .setGraph(R.navigation.nav_graph)
                    .setDestination(R.id.galleryFragment)
                    .setArguments(bundle)
                    .createPendingIntent()

            //val appPendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT)//
            views.setPendingIntentTemplate(R.id.widget_grid_view, pendingIntent)
            return views
        }
    }
}