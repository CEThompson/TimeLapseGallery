package com.vwoom.timelapsegallery.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.vwoom.timelapsegallery.R;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.utils.Keys;
import com.vwoom.timelapsegallery.utils.PhotoUtils;
import com.vwoom.timelapsegallery.utils.ProjectUtils;
import com.vwoom.timelapsegallery.utils.TimeUtils;

import java.util.List;

public class WidgetGridRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private List<ProjectEntry> mProjects;

    private static final String TAG = WidgetGridRemoteViewsFactory.class.getSimpleName();

    public WidgetGridRemoteViewsFactory(Context applicationContext, Intent intent){
        mContext = applicationContext;
        mProjects = null;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        /* Load the projects for the day */
        TimeLapseDatabase timeLapseDatabase = TimeLapseDatabase.getInstance(mContext);
        List<ProjectEntry> allScheduledProjects = timeLapseDatabase.projectDao().loadAllScheduledProjects();
        mProjects = ProjectUtils.getProjectsScheduledToday(allScheduledProjects);
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        if (mProjects == null) return 0;
        else return mProjects.size();
    }

    @Override
    public RemoteViews getViewAt(int i) {
        // Get the current project
        ProjectEntry currentProject = mProjects.get(i);
        long nextSubmissionTime = TimeUtils.getNextScheduledSubmission(currentProject.getSchedule_next_submission(), currentProject.getSchedule());

        // Get strings
        String nextSubmissionTimeString = TimeUtils.getTimeFromTimestamp(nextSubmissionTime);

        // Create the remote views
        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);

        // Decode the bitmap from path
        Bitmap bitmap = PhotoUtils.decodeSampledBitmapFromPath(currentProject.getThumbnail_url(), 100, 100);

        // Rotate the bitmap
        Integer bitmapOrientation = PhotoUtils.getOrientationFromImagePath(currentProject.getThumbnail_url());
        bitmap = PhotoUtils.rotateBitmap(bitmap, bitmapOrientation);

        // Set the view strings
        views.setTextViewText(R.id.widget_list_item_name_text_view, currentProject.getName());
        views.setTextViewText(R.id.widget_list_item_time_text_view, nextSubmissionTimeString);
        views.setImageViewBitmap(R.id.widget_list_item_image_view, bitmap);

        // Send the project in an intent
        Bundle extras = new Bundle();
        extras.putParcelable(Keys.PROJECT_ENTRY, currentProject);
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        views.setOnClickFillInIntent(R.id.widget_grid_item_layout, fillInIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

}
