package com.vwoom.timelapsegallery.gallery

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.gallery.GalleryAdapter.GalleryAdapterViewHolder
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.PhotoUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils.getEntryFromProject
import com.vwoom.timelapsegallery.utils.TimeUtils
import java.io.File
import java.util.*
import kotlin.math.log

class GalleryAdapter(private val mClickHandler: GalleryAdapterOnClickHandler, val externalFilesDir: File) : RecyclerView.Adapter<GalleryAdapterViewHolder>() {
    private var mProjectData: List<Project>? = null
    private var mProjectsToCoverPhotos: HashMap<Project, File> = hashMapOf()
    private var mCoverPhotosToRatios: HashMap<File, String> = hashMapOf()
    private val constraintSet: ConstraintSet = ConstraintSet()

    interface GalleryAdapterOnClickHandler {
        fun onClick(clickedProject: Project, binding: GalleryRecyclerviewItemBinding, position: Int)
    }

    inner class GalleryAdapterViewHolder(val binding: GalleryRecyclerviewItemBinding)
        : RecyclerView.ViewHolder(binding.root), OnClickListener {
        override fun onClick(view: View) {
            val adapterPosition = adapterPosition
            val clickedProject = mProjectData!![adapterPosition]
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
        val project = mProjectData!![position]
        val photoFile = mProjectsToCoverPhotos[project]

        // Set the constraint ratio
        val ratio = mCoverPhotosToRatios[photoFile]
        constraintSet.clone(holder.binding.projectRecyclerviewConstraintLayout)
        constraintSet.setDimensionRatio(holder.binding.projectImage.id, ratio)
        constraintSet.applyTo(holder.binding.projectRecyclerviewConstraintLayout)

        // Handle Check Display
        val photoTakenToday = DateUtils.isToday(project.cover_photo_timestamp)
        if (photoTakenToday) {
            holder.binding.scheduleIndicatorCheck.visibility = VISIBLE
        } else {
            holder.binding.scheduleIndicatorCheck.visibility = INVISIBLE
        }

        val projectIsScheduled = (project.interval_days != 0)
        if (projectIsScheduled){
            setScheduleInformation(project, holder, project.interval_days, photoTakenToday)
            holder.binding.galleryScheduleLayout.scheduleLayout.visibility = VISIBLE
        } else {
            holder.binding.galleryScheduleLayout.scheduleLayout.visibility = INVISIBLE
        }

        // Set transition targets
        val imageTransitionName = project.project_id.toString()
        val cardTransitionName = "${imageTransitionName}card"
        val bottomGradientTransitionName = "${imageTransitionName}bottomGradient"
        val topGradientTransitionName = "${imageTransitionName}topGradient"
        val dueTransitionName = "${imageTransitionName}due"
        val intervalTransitionName = "${imageTransitionName}interval"
        holder.binding.projectImage.transitionName = imageTransitionName
        holder.binding.projectCardView.transitionName = cardTransitionName
        holder.binding.galleryBottomGradient.transitionName = bottomGradientTransitionName
        holder.binding.galleryScheduleLayout.galleryGradientTopDown.transitionName = topGradientTransitionName
        holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv.transitionName = dueTransitionName
        holder.binding.galleryScheduleLayout.scheduleIndicatorIntervalTv.transitionName = intervalTransitionName


        // Load the image
        Glide.with(holder.itemView.context)
                .load(photoFile)
                .into(holder.binding.projectImage)
    }

    override fun getItemCount(): Int {
        return if (mProjectData == null) 0 else mProjectData!!.size
    }

    // TODO: (update 1.2) consider calculating this information somewhere else to speed up the gallery (perhaps a diff util?)
    fun setProjectData(projectData: List<Project>) {
        mProjectData = projectData
        mProjectsToCoverPhotos.clear()
        for (project in projectData) {
            val photoUrl = FileUtils.getPhotoUrl(
                    externalFilesDir,
                    getEntryFromProject(project),
                    project.cover_photo_timestamp)
            val ratio = PhotoUtils.getAspectRatioFromImagePath(photoUrl)
            val file = File(photoUrl)
            mProjectsToCoverPhotos.apply { put(project, file) }
            mCoverPhotosToRatios.apply { put(file, ratio) }
        }
        notifyDataSetChanged()
    }

    // Updates the UI for the schedule layout
    private fun setScheduleInformation(project: Project, holder: GalleryAdapterViewHolder, interval_days: Int, photoTakenToday: Boolean) {
        // Calc the days until project is due
        val daysSinceLastPhoto = TimeUtils.getDaysSinceTimeStamp(project.cover_photo_timestamp, System.currentTimeMillis())
        val daysUntilDue = project.interval_days - daysSinceLastPhoto
        holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv.text = daysUntilDue.toString() // set days until due text
        holder.binding.galleryScheduleLayout.scheduleIndicatorIntervalTv.text = interval_days.toString() // set schedule interval

        // Calc opacity for due date
        when {
            photoTakenToday -> holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv.alpha = 0.3f
            daysUntilDue <= 0 -> holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv.alpha = 1f
            daysUntilDue == 1.toLong() ->  holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv.alpha = .9f
            else -> {
                val minOpacity = .5f
                val dimFactor = (1f/log(daysUntilDue.toFloat(),2f))
                val opacityAdjust = .4f * dimFactor
                val opacity = minOpacity + opacityAdjust
                holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv.alpha = opacity
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

    companion object {
        private val TAG = GalleryAdapter::class.java.simpleName
    }
}