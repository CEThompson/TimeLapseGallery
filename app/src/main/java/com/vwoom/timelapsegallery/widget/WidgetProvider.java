package com.vwoom.timelapsegallery.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.vwoom.timelapsegallery.R;
import com.vwoom.timelapsegallery.activities.DetailsActivity;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;

import java.util.List;

public class WidgetProvider extends AppWidgetProvider {

    public static void updateWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, List<ProjectEntry> projects) {
        for (int appWidgetId : appWidgetIds){
            updateAppWidget(context, appWidgetManager, appWidgetId, projects);
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, List<ProjectEntry> projects){
        RemoteViews views;

        // If there are no projects handle the layout here
        if (projects.size() == 0){
            views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            views.setTextViewText(R.id.widget_text_view, context.getString(R.string.no_projects_for_today));
            views.setViewVisibility(R.id.widget_text_view, View.VISIBLE);
            views.setViewVisibility(R.id.widget_grid_item_layout, View.INVISIBLE);
        }

        // Otherwise set the list of upcoming project submissions
        else {
            views = getGridRemoteViews(context);
            views.setViewVisibility(R.id.widget_text_view, View.INVISIBLE);
            views.setViewVisibility(R.id.widget_grid_item_layout, View.VISIBLE);
        }

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        UpdateWidgetService.startActionUpdateWidgets(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    /* Creates the grid of projects for the widget */
    private static RemoteViews getGridRemoteViews(Context context){
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        Intent adapterIntent = new Intent(context, WidgetGridRemoteViewsService.class);

        views.setRemoteAdapter(R.id.widget_list_view, adapterIntent);

        Intent appIntent = new Intent(context, DetailsActivity.class);

        PendingIntent appPendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.widget_list_view, appPendingIntent);

        return views;
    }
}
