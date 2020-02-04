package com.vwoom.timelapsegallery.gallery

import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.gallery.GalleryAdapter.GalleryAdapterViewHolder
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.PhotoUtils
import com.vwoom.timelapsegallery.utils.TimeUtils
import java.io.File
import java.util.*

class GalleryAdapter(private val mClickHandler: GalleryAdapterOnClickHandler, val externalFilesDir: File) : RecyclerView.Adapter<GalleryAdapterViewHolder>() {
    private var mProjectData: List<Project>? = null
    private val constraintSet: ConstraintSet? = ConstraintSet()

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
        val currentProject = mProjectData!![position]
        val binding = holder.binding

        val thumbnailPath = FileUtils.getCoverPhotoUrl(this.externalFilesDir, currentProject)
        Log.d(TAG, "thumbnail_path is $thumbnailPath")
        // Set the constraint ratio
        val ratio = PhotoUtils.getAspectRatioFromImagePath(thumbnailPath)
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

        // Set transition targets
        val imageTransitionName = currentProject.project_id.toString()
        val cardTransitionName = imageTransitionName + "card"
        binding.projectImage.transitionName = imageTransitionName
        binding.projectCardView.transitionName = cardTransitionName

        Log.d(TAG, "tracking transition: detail fragment $imageTransitionName & $cardTransitionName")
        // Load the image
        val f = File(thumbnailPath)
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
}