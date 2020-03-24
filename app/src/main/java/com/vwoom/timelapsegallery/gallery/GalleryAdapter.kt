package com.vwoom.timelapsegallery.gallery

import android.text.format.DateUtils
import android.util.Log
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
import kotlin.math.log10

class GalleryAdapter(private val mClickHandler: GalleryAdapterOnClickHandler, val externalFilesDir: File) : RecyclerView.Adapter<GalleryAdapterViewHolder>() {
    private var mProjectData: List<Project>? = null
    private var mProjectsToCoverPhotos: HashMap<Project, File> = hashMapOf()
    private var mCoverPhotosToRatios: HashMap<File, String> = hashMapOf()
    private val constraintSet: ConstraintSet = ConstraintSet()

    interface GalleryAdapterOnClickHandler {
        fun onClick(clickedProject: Project, projectImageView: ImageView, projectCardView: CardView, position: Int)
    }

    inner class GalleryAdapterViewHolder(var binding: GalleryRecyclerviewItemBinding)
        : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        override fun onClick(view: View) {
            val adapterPosition = adapterPosition
            val clickedProject = mProjectData!![adapterPosition]
            mClickHandler.onClick(
                    clickedProject,
                    binding.projectImage,
                    binding.projectCardView,
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
            holder.binding.galleryItemCheckLayout.visibility = VISIBLE
        } else {
            holder.binding.galleryItemCheckLayout.visibility = INVISIBLE
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
        holder.binding.projectImage.transitionName = imageTransitionName
        holder.binding.projectCardView.transitionName = cardTransitionName

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
        holder.binding.galleryScheduleLayout.daysUntilDueTextView.text = daysUntilDue.toString() // set days until due text
        holder.binding.galleryScheduleLayout.galleryItemScheduleIndicatorDays.text = interval_days.toString() // set schedule interval

        // Calc opacity for due date
        when {
            photoTakenToday -> holder.binding.galleryScheduleLayout.daysUntilDueTextView.alpha = 0.3f
            daysUntilDue <= 0 -> holder.binding.galleryScheduleLayout.daysUntilDueTextView.alpha = 1f
            daysUntilDue == 1.toLong() ->  holder.binding.galleryScheduleLayout.daysUntilDueTextView.alpha = .9f
            else -> {
                val minOpacity = .5f
                val dimFactor = (1f/log(daysUntilDue.toFloat(),2f))
                val opacityAdjust = .4f * dimFactor
                val opacity = minOpacity + opacityAdjust
                holder.binding.galleryScheduleLayout.daysUntilDueTextView.alpha = opacity
            }
        }

        // Style depending upon due state
        if (daysUntilDue <= 0) {
            holder.binding.galleryScheduleLayout.daysUntilDueTextView
                    .setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorSubtleRedAccent))
            holder.binding.galleryScheduleLayout.scheduleIconDue.visibility = VISIBLE
            holder.binding.galleryScheduleLayout.scheduleIconPending.visibility = INVISIBLE
        } else {
            holder.binding.galleryScheduleLayout.daysUntilDueTextView
                    .setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            holder.binding.galleryScheduleLayout.scheduleIconDue.visibility = INVISIBLE
            holder.binding.galleryScheduleLayout.scheduleIconPending.visibility = VISIBLE
        }
    }

    companion object {
        private val TAG = GalleryAdapter::class.java.simpleName
    }
}