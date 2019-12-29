package com.vwoom.timelapsegallery.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.TimeLapseDatabase.Companion.getInstance
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.utils.FileUtils.getPhotoUrl
import com.vwoom.timelapsegallery.utils.PhotoUtils.decodeSampledBitmapFromPath
import com.vwoom.timelapsegallery.utils.PhotoUtils.getOrientationFromImagePath
import com.vwoom.timelapsegallery.utils.PhotoUtils.rotateBitmap
import com.vwoom.timelapsegallery.utils.TimeUtils.getNextScheduledSubmission
import com.vwoom.timelapsegallery.utils.TimeUtils.getTimeFromTimestamp
import java.io.File
import java.io.IOException

class WidgetGridRemoteViewsFactory(private val mContext: Context, intent: Intent?) : RemoteViewsFactory {
    private var mProjects: List<ProjectEntry>?
    private val mTimeLapseDatabase: TimeLapseDatabase
    private val mExternalFilesDir: File?
    override fun onCreate() {}
    override fun onDataSetChanged() { /* Load the projects for the day */
        mProjects = mTimeLapseDatabase.projectDao().loadAllScheduledProjects()
    }

    override fun onDestroy() {}
    override fun getCount(): Int {
        return if (mProjects == null) 0 else mProjects!!.size
    }

    override fun getViewAt(i: Int): RemoteViews { // Get the current project
        val currentProject = mProjects!![i]
        val (_, schedule_time, interval_days) = mTimeLapseDatabase.projectScheduleDao().loadScheduleByProjectId(currentProject.id)
        val (_, photo_id) = mTimeLapseDatabase.coverPhotoDao().getCoverPhoto_nonLiveData(currentProject.id)
        val coverPhoto = mTimeLapseDatabase.photoDao().loadPhoto(currentProject.id, photo_id)
        val nextSubmissionTime = getNextScheduledSubmission(schedule_time!!, interval_days!!)
        // Get strings
        val nextSubmissionTimeString = getTimeFromTimestamp(nextSubmissionTime)
        // Create the remote views
        val views = RemoteViews(mContext.packageName, R.layout.widget_list_item)
        val coverPhotoPath = getPhotoUrl(mExternalFilesDir!!, currentProject, coverPhoto)
        // Decode the bitmap from path
        var bitmap: Bitmap? = decodeSampledBitmapFromPath(
                coverPhotoPath,
                100,
                100)
        // Rotate the bitmap
        try {
            val bitmapOrientation = getOrientationFromImagePath(coverPhotoPath)
            bitmap = rotateBitmap(bitmap!!, bitmapOrientation)
        } catch (e: IOException) {
            if (e.message != null) Log.e(TAG, e.message)
        }
        // Set the view strings
        views.setTextViewText(R.id.widget_list_item_name_text_view, currentProject.project_name)
        views.setTextViewText(R.id.widget_list_item_time_text_view, nextSubmissionTimeString)
        views.setImageViewBitmap(R.id.widget_list_item_image_view, bitmap)
        // Send the project in an intent
        val extras = Bundle()
        // TODO determine what to pass here!
//extras.putParcelable(Keys.PROJECT, currentProject);
        val fillInIntent = Intent()
        fillInIntent.putExtras(extras)
        views.setOnClickFillInIntent(R.id.widget_grid_item_layout, fillInIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    companion object {
        private val TAG = WidgetGridRemoteViewsFactory::class.java.simpleName
    }

    init {
        mExternalFilesDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        mProjects = null
        mTimeLapseDatabase = getInstance(mContext)
    }
}