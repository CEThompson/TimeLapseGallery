package com.vwoom.timelapsegallery.gallery

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.gallery.GalleryAdapter.GalleryAdapterViewHolder
import com.vwoom.timelapsegallery.utils.PhotoUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils.getProjectEntryFromProjectView
import com.vwoom.timelapsegallery.utils.TimeUtils
import java.io.File
import java.util.*

class GalleryAdapter(
        private val mClickHandler: GalleryAdapterOnClickHandler,
        val externalFilesDir: File,
        private val scheduleDisplaysEnabled: Boolean) : RecyclerView.Adapter<GalleryAdapterViewHolder>() {
    private var mProjectViewData: List<ProjectView> = listOf()
    private var mProjectsToCoverPhotos: HashMap<ProjectView, File> = hashMapOf()
    private var mCoverPhotosToRatios: HashMap<File, String> = hashMapOf()
    private val constraintSet: ConstraintSet = ConstraintSet()

    interface GalleryAdapterOnClickHandler {
        fun onClick(clickedProjectView: ProjectView, binding: GalleryRecyclerviewItemBinding, position: Int)
    }

    inner class GalleryAdapterViewHolder(val binding: GalleryRecyclerviewItemBinding)
        : RecyclerView.ViewHolder(binding.root), OnClickListener {
        override fun onClick(view: View) {
            val adapterPosition = adapterPosition
            val clickedProject = mProjectViewData[adapterPosition]
            mClickHandler.onClick(
                    clickedProject,
                    binding,
                    adapterPosition)
        }

        init {
            binding.root.setOnClickListener(this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryAdapterViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val shouldAttachToParentImmediately = false

        val binding = GalleryRecyclerviewItemBinding.inflate(inflater, parent, shouldAttachToParentImmediately)
        return GalleryAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GalleryAdapterViewHolder, position: Int) {
        // Get project information
        val project: ProjectView? = mProjectViewData[position]
        val photoFile: File? = mProjectsToCoverPhotos[project]
        // Set the constraint ratio
        var ratio: String? = mCoverPhotosToRatios[photoFile]
        if (ratio == null) ratio = holder.itemView.context.getString(R.string.default_aspect_ratio)

        constraintSet.clone(holder.binding.projectRecyclerviewConstraintLayout)
        constraintSet.setDimensionRatio(holder.binding.projectImage.id, ratio)
        constraintSet.applyTo(holder.binding.projectRecyclerviewConstraintLayout)

        // Handle Check Display
        val photoTakenToday = if (project == null || photoFile == null) false else DateUtils.isToday(project.cover_photo_timestamp)
        if (photoTakenToday) {
            holder.binding.scheduleIndicatorCheck.visibility = VISIBLE
        } else {
            holder.binding.scheduleIndicatorCheck.visibility = INVISIBLE
        }

        val projectIsScheduled = if (project == null) false else (project.interval_days != 0)
        if (projectIsScheduled && scheduleDisplaysEnabled && project != null) {
            setScheduleInformation(project, holder, project.interval_days, photoTakenToday)
            holder.binding.galleryScheduleLayout.scheduleLayout.visibility = VISIBLE
        } else {
            holder.binding.galleryScheduleLayout.scheduleLayout.visibility = INVISIBLE
        }

        // Set transition targets
        val imageTransitionName = project?.project_id?.toString() ?: "projectLoadError"
        holder.binding.projectImage.transitionName = imageTransitionName
        holder.binding.projectCardView.transitionName = "${imageTransitionName}card"
        holder.binding.galleryBottomGradient.transitionName = "${imageTransitionName}bottomGradient"
        holder.binding.galleryScheduleLayout.galleryGradientTopDown.transitionName = "${imageTransitionName}topGradient"
        holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv.transitionName = "${imageTransitionName}due"
        holder.binding.galleryScheduleLayout.scheduleIndicatorIntervalTv.transitionName = "${imageTransitionName}interval"

        if (photoFile == null) {
            Glide.with(holder.itemView.context)
                    .load(R.drawable.ic_sentiment_very_dissatisfied_white_24dp)
                    .centerInside()
                    .into(holder.binding.projectImage)
        } else {
            // Load the image
            Glide.with(holder.itemView.context)
                    .load(photoFile)
                    .error(R.drawable.ic_sentiment_very_dissatisfied_white_24dp)
                    .into(holder.binding.projectImage)
        }
    }

    override fun getItemCount(): Int {
        return mProjectViewData.size
    }

    // TODO: (update 1.2) consider calculating this information somewhere else to speed up the gallery (perhaps a diff util?)
    fun setProjectData(projectViewData: List<ProjectView>) {
        mProjectViewData = projectViewData
        mProjectsToCoverPhotos.clear()
        for (project in projectViewData) {
            val photoUrl = ProjectUtils.getProjectPhotoUrl(
                    externalFilesDir,
                    getProjectEntryFromProjectView(project),
                    project.cover_photo_timestamp)
            if (photoUrl != null) {
                val ratio = PhotoUtils.getAspectRatioFromImagePath(photoUrl)
                val file = File(photoUrl)
                mCoverPhotosToRatios.apply { put(file, ratio) }
                mProjectsToCoverPhotos.apply { put(project, file) }
            }
        }
        notifyDataSetChanged()
    }

    // Updates the UI for the schedule layout
    private fun setScheduleInformation(projectView: ProjectView, holder: GalleryAdapterViewHolder, interval_days: Int, photoTakenToday: Boolean) {
        // Calc the days until project is due
        val daysSinceLastPhoto = TimeUtils.getDaysSinceTimeStamp(projectView.cover_photo_timestamp, System.currentTimeMillis())
        val daysUntilDue = projectView.interval_days - daysSinceLastPhoto
        holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv.text = daysUntilDue.toString() // set days until due text
        holder.binding.galleryScheduleLayout.scheduleIndicatorIntervalTv.text = interval_days.toString() // set schedule interval

        // Calc opacity for due date
        when {
            // De-emphasize the schedule if the photo was already taken
            photoTakenToday -> {
                holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv.alpha = 0.3f
                holder.binding.galleryScheduleLayout.scheduleIndicatorIntervalTv.alpha = 0.3f
            }
            // Set at full strength if the project is due
            daysUntilDue <= 0 -> {
                holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv.alpha = 1f
                holder.binding.galleryScheduleLayout.scheduleIndicatorIntervalTv.alpha = 1f
            }
            // Indicate the project is coming up tomorrow
            daysUntilDue == 1.toLong() -> {
                holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv.alpha = .9f
                holder.binding.galleryScheduleLayout.scheduleIndicatorIntervalTv.alpha = 0.9f
            }
            else -> {
                // Calculate the opacity for mininum of 0.3f and maximum of 0.9f
                val minOpacity = .3f
                val maxFallOff = 0.6f
                // opacity falls off at adjustment of 1/N to a minimum of 0.3f
                val calcOpacity = maxFallOff * (1f / daysUntilDue.toFloat()) + minOpacity
                holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv.alpha = calcOpacity
                holder.binding.galleryScheduleLayout.scheduleIndicatorIntervalTv.alpha = calcOpacity
            }
        }

        // Style depending upon due state
        if (daysUntilDue <= 0) {
            holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv
                    .setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorSubtleRedAccent))
            holder.binding.galleryScheduleLayout.scheduleIndicatorIntervalTv
                    .setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_schedule_indicator_due_24dp, 0, 0, 0)
        } else {
            holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv
                    .setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            holder.binding.galleryScheduleLayout.scheduleIndicatorIntervalTv
                    .setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_schedule_indicator_pending_24dp, 0, 0, 0)
        }
    }
}