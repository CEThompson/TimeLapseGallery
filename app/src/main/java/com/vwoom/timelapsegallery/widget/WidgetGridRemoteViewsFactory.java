package com.vwoom.timelapsegallery.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.vwoom.timelapsegallery.R;
import com.vwoom.timelapsegallery.database.entry.CoverPhotoEntry;
import com.vwoom.timelapsegallery.database.entry.PhotoEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.database.entry.ProjectScheduleEntry;
import com.vwoom.timelapsegallery.utils.FileUtils;
import com.vwoom.timelapsegallery.utils.Keys;
import com.vwoom.timelapsegallery.utils.PhotoUtils;
import com.vwoom.timelapsegallery.utils.TimeUtils;

import java.io.IOException;
import java.util.List;

public class WidgetGridRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private List<ProjectEntry> mProjects;
    private TimeLapseDatabase mTimeLapseDatabase;
    private static final String TAG = WidgetGridRemoteViewsFactory.class.getSimpleName();

    public WidgetGridRemoteViewsFactory(Context applicationContext, Intent intent){
        mContext = applicationContext;
        mProjects = null;
        mTimeLapseDatabase = TimeLapseDatabase.getInstance(mContext);
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        /* Load the projects for the day */
        mProjects = mTimeLapseDatabase.projectDao().loadAllScheduledProjects();

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
        ProjectScheduleEntry schedule = mTimeLapseDatabase.projectScheduleDao().loadScheduleByProjectId(currentProject.getId());
        CoverPhotoEntry coverPhotoEntry = mTimeLapseDatabase.coverPhotoDao().getCoverPhoto_nonLiveData(currentProject.getId());
        PhotoEntry coverPhoto = mTimeLapseDatabase.photoDao().loadPhoto(currentProject.getId(), coverPhotoEntry.getPhoto_id());

        long nextSubmissionTime = TimeUtils.getNextScheduledSubmission(schedule.getSchedule_time(), schedule.getInterval_days());

        // Get strings
        String nextSubmissionTimeString = TimeUtils.getTimeFromTimestamp(nextSubmissionTime);

        // Create the remote views
        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);

        String coverPhotoPath = FileUtils.getPhotoUrl(mContext, currentProject, coverPhoto);
        // Decode the bitmap from path
        Bitmap bitmap = PhotoUtils.decodeSampledBitmapFromPath(
                coverPhotoPath,
                100,
                100);

        // Rotate the bitmap
        try {
            Integer bitmapOrientation = PhotoUtils.getOrientationFromImagePath(coverPhotoPath);
            bitmap = PhotoUtils.rotateBitmap(bitmap, bitmapOrientation);
        } catch (IOException e){
            if (e.getMessage()!=null)
                Log.e(TAG, e.getMessage());
        }

        // Set the view strings
        views.setTextViewText(R.id.widget_list_item_name_text_view, currentProject.getProject_name());
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
