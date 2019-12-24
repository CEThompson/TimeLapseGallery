package com.vwoom.timelapsegallery.gallery

import android.content.Context
import android.os.Environment
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.gallery.GalleryAdapter.GalleryAdapterViewHolder
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.PhotoUtils
import com.vwoom.timelapsegallery.utils.TimeUtils
import java.io.File
import java.util.*

class GalleryAdapter(private val mClickHandler: GalleryAdapterOnClickHandler, context: Context) : RecyclerView.Adapter<GalleryAdapterViewHolder>() {
    private var mProjectData: List<Project>? = null
    private val constraintSet: ConstraintSet? = ConstraintSet()
    private val mExternalFilesDir: File?

    interface GalleryAdapterOnClickHandler {
        fun onClick(clickedProject: Project, sharedElement: View, transitionName: String, position: Int)
    }

    inner class GalleryAdapterViewHolder(var binding: GalleryRecyclerviewItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        override fun onClick(view: View) {
            val adapterPosition = adapterPosition
            val clickedProject = mProjectData!![adapterPosition]
            val transitionName = binding.projectCardView.transitionName
            mClickHandler.onClick(clickedProject, binding.projectCardView, transitionName, adapterPosition)
        }

        init {
            binding.root.setOnClickListener(this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryAdapterViewHolder {
        val context = parent.context
        //val layoutIdForGridItem = R.layout.gallery_recyclerview_item
        val inflater = LayoutInflater.from(context)
        val shouldAttachToParentImmediately = false

        val binding = GalleryRecyclerviewItemBinding.inflate(inflater, parent, shouldAttachToParentImmediately)
        //val view = inflater.inflate(layoutIdForGridItem, parent, shouldAttachToParentImmediately)
        //return ProjectsAdapterViewHolder(view)
        return GalleryAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GalleryAdapterViewHolder, position: Int) { // Get project information
        val currentProject = mProjectData!![position]
        val binding = holder.binding

        // TODO remove these logs
        // Logs for project information
        val project_id = currentProject.project_id
        Log.d(TAG, "project_id is " + project_id)
        val project_name = currentProject.project_name
        Log.d(TAG, "project name is " + project_name)
        val cover_set_by_user = currentProject.cover_set_by_user
        Log.d(TAG, "cover set by user is " + cover_set_by_user)
        val schedule_time = currentProject.schedule_time
        Log.d(TAG, "schedule time is " + schedule_time)
        val interval_days = currentProject.interval_days
        Log.d(TAG, "interval days is " + interval_days)
        val cover_photo_id = currentProject.cover_photo_id
        Log.d(TAG, "cover photo id is " + cover_photo_id)
        val cover_photo_timestamp = currentProject.cover_photo_timestamp
        Log.d(TAG, "cover photo timestamp is " + cover_photo_timestamp)

        // TODO test photo url from hashmap
        val thumbnail_path = FileUtils.getCoverPhotoUrl(mExternalFilesDir, currentProject)
        Log.d(TAG, "thumbnail_path is $thumbnail_path")
        // Set the constraint ratio
        val ratio = PhotoUtils.getAspectRatioFromImagePath(thumbnail_path)
        constraintSet!!.clone(binding.projectRecyclerviewConstraintLayout)
        constraintSet.setDimensionRatio(binding.projectImage.id, ratio)
        constraintSet.applyTo(binding.projectRecyclerviewConstraintLayout)
        // Display schedule information
        // TODO test schedule information
        val next = currentProject.schedule_time
        val interval = currentProject.interval_days
        if (next != null && interval != null) {
            val nextSchedule: String
            // Calculate day countdown
            val cal = Calendar.getInstance()
            cal.timeInMillis = System.currentTimeMillis()
            val currentDay = cal[Calendar.DAY_OF_YEAR]
            cal.timeInMillis = next
            val scheduledDay = cal[Calendar.DAY_OF_YEAR]
            val daysUntilPhoto = scheduledDay - currentDay
            // Handle projects scheduled for today
            nextSchedule = if (DateUtils.isToday(next) || System.currentTimeMillis() > next) TimeUtils.getTimeFromTimestamp(next) else if (daysUntilPhoto == 1) holder.itemView.context.getString(R.string.tomorrow) else holder.itemView.context.getString(R.string.number_of_days, daysUntilPhoto)
            // Set fields
            binding.nextSubmissionDayCountdownTextview.text = nextSchedule
            // Set visibility
            binding.scheduleIndicator.visibility = View.VISIBLE
            binding.nextSubmissionDayCountdownTextview.visibility = View.VISIBLE
            binding.projectImageGradientOverlay.visibility = View.VISIBLE
        } else {
            binding.scheduleIndicator.visibility = View.INVISIBLE
            binding.nextSubmissionDayCountdownTextview.visibility = View.INVISIBLE
            binding.projectImageGradientOverlay.visibility = View.INVISIBLE
        }
        // Set the transition name
        val transitionName = currentProject.project_id.toString() + currentProject.project_name
        binding.projectCardView.transitionName = transitionName
        // TODO set the transition name to the photo url
        binding.projectCardView.setTag(R.string.transition_tag, thumbnail_path)
        // Load the image
        val f = File(thumbnail_path)
        Glide.with(holder.itemView.context)
                .load(f)
                .into(binding.projectImage)
    }

    override fun getItemCount(): Int {
        return if (mProjectData == null) 0 else mProjectData!!.size
    }

    fun setProjectData(projectData: List<Project>?) {
        mProjectData = projectData
        notifyDataSetChanged()
    }

    companion object {
        private val TAG = GalleryAdapter::class.java.simpleName
    }

    init {
        mExternalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    }
}