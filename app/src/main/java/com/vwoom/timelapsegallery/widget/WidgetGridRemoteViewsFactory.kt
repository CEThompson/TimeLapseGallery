package com.vwoom.timelapsegallery.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.text.format.DateUtils
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.TimeLapseDatabase.Companion.getInstance
import com.vwoom.timelapsegallery.data.entry.CoverPhotoEntry
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.repository.CoverPhotoRepository
import com.vwoom.timelapsegallery.data.repository.PhotoRepository
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.repository.ProjectScheduleRepository
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.FileUtils.getPhotoUrl
import com.vwoom.timelapsegallery.utils.PhotoUtils.decodeSampledBitmapFromPath
import com.vwoom.timelapsegallery.utils.PhotoUtils.getOrientationFromImagePath
import com.vwoom.timelapsegallery.utils.PhotoUtils.rotateBitmap
import com.vwoom.timelapsegallery.utils.TimeUtils.getNextScheduledSubmission
import com.vwoom.timelapsegallery.utils.TimeUtils.getTimeFromTimestamp
import java.io.File
import java.io.IOException

class WidgetGridRemoteViewsFactory(
        private val mContext: Context,
        intent: Intent?,
        private val projectRepository: ProjectRepository,
        private val projectScheduleRepository: ProjectScheduleRepository,
        private val coverPhotoRepository: CoverPhotoRepository,
        private val photoRepository: PhotoRepository) : RemoteViewsFactory {

    private var mProjects: List<ProjectEntry>
    private var mExternalFilesDir: File? = null

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // Load the projects for the day
        var projects = projectRepository.getScheduledProjects()

        // Filter scheduled projects for today
        projects = projects.filter {
            val schedule = projectScheduleRepository.getProjectScheduleNonSuspend(it.id)
            if (schedule?.schedule_time == null) return@filter false
            else return@filter DateUtils.isToday(schedule?.schedule_time!!)
        }
        mProjects = projects
    }

    override fun onDestroy() {}
    override fun getCount(): Int {
        return mProjects.size
    }

    override fun getViewAt(i: Int): RemoteViews { // Get the current project
        val currentProject: ProjectEntry = mProjects!![i]
        val projectSchedule: ProjectScheduleEntry? = projectScheduleRepository.getProjectScheduleNonSuspend(currentProject.id)
        val coverPhoto: CoverPhotoEntry = coverPhotoRepository.getCoverPhoto(currentProject.id)
        val photoEntry: PhotoEntry = photoRepository.getPhoto(currentProject.id, coverPhoto.photo_id)

        val nextSubmissionTime = getNextScheduledSubmission(projectSchedule?.schedule_time!!, projectSchedule?.interval_days!!)
        // Get strings
        val nextSubmissionTimeString = getTimeFromTimestamp(nextSubmissionTime)
        // Create the remote views
        val views = RemoteViews(mContext.packageName, R.layout.widget_list_item)
        val coverPhotoPath = getPhotoUrl(mExternalFilesDir!!, currentProject, photoEntry)
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
        // TODO determine what to pass here so that clicking widget takes user to project
        // TODO determine how this is different from usage in widget provider
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
        mProjects = arrayListOf()
    }
}