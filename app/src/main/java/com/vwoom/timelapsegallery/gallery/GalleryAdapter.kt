package com.vwoom.timelapsegallery.gallery

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.gallery.GalleryAdapter.GalleryAdapterViewHolder
import com.vwoom.timelapsegallery.utils.PhotoUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils.getProjectEntryFromProjectView
import com.vwoom.timelapsegallery.utils.TimeUtils
import java.io.File

class GalleryAdapter(
        private val mClickHandler: GalleryAdapterOnClickHandler,
        val externalFilesDir: File,
        private val scheduleDisplaysEnabled: Boolean,
        private val gifDisplaysEnabled: Boolean)
    : ListAdapter<ProjectView, GalleryAdapterViewHolder>(ProjectViewDiffCallback()) {
    private val constraintSet: ConstraintSet = ConstraintSet()

    interface GalleryAdapterOnClickHandler {
        fun onClick(clickedProjectView: ProjectView, binding: GalleryRecyclerviewItemBinding, position: Int)
    }

    inner class GalleryAdapterViewHolder(val binding: GalleryRecyclerviewItemBinding)
        : RecyclerView.ViewHolder(binding.root), OnClickListener {
        override fun onClick(view: View) {
            val adapterPosition = adapterPosition
            val clickedProject = getItem(adapterPosition)
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
        val project: ProjectView = getItem(position)

        // Get the photo for the project
        val photoUrl = ProjectUtils.getProjectPhotoUrl(
                externalFilesDir,
                getProjectEntryFromProjectView(project),
                project.cover_photo_timestamp)

        // Initialize a constraint ratio
        var ratio: String = holder.itemView.context.getString(R.string.default_aspect_ratio)
        // Get the aspect ratio from the photo if available
        if (photoUrl != null) ratio = PhotoUtils.getAspectRatioFromImagePath(photoUrl)

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

        // Handle the schedule layout
        val projectIsScheduled = (project.interval_days != 0)
        if (projectIsScheduled && scheduleDisplaysEnabled) {
            setScheduleInformation(project, holder, project.interval_days, photoTakenToday)
            holder.binding.galleryScheduleLayout.scheduleLayout.visibility = VISIBLE
        } else {
            holder.binding.galleryScheduleLayout.scheduleLayout.visibility = INVISIBLE
        }

        // Set transition targets
        val imageTransitionName = project.project_id.toString()
        holder.binding.projectImage.transitionName = imageTransitionName
        holder.binding.projectCardView.transitionName = "${imageTransitionName}card"
        holder.binding.galleryBottomGradient.transitionName = "${imageTransitionName}bottomGradient"
        holder.binding.galleryScheduleLayout.galleryGradientTopDown.transitionName = "${imageTransitionName}topGradient"
        holder.binding.galleryScheduleLayout.scheduleDaysUntilDueTv.transitionName = "${imageTransitionName}due"
        holder.binding.galleryScheduleLayout.scheduleIndicatorIntervalTv.transitionName = "${imageTransitionName}interval"

        // If not photo load an error
        if (photoUrl == null) {
            Glide.with(holder.itemView.context)
                    .load(R.drawable.ic_sentiment_very_dissatisfied_white_24dp)
                    .centerInside()
                    .into(holder.binding.projectImage)
        }
        // Otherwise the gif for the project if created
        else if (gifDisplaysEnabled) {
            val gifFile = ProjectUtils.getGifForProject(externalFilesDir, getProjectEntryFromProjectView(project))
            if (gifFile != null) {
                // TODO: figure out why gif is intermittently stuck on first frame after return
                Glide.with(holder.itemView.context)
                        .asGif()
                        .load(gifFile)
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)).listener(object : RequestListener<GifDrawable> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<GifDrawable>?, isFirstResource: Boolean): Boolean {
                                loadImage(holder, photoUrl)
                                return false
                            }

                            override fun onResourceReady(resource: GifDrawable?, model: Any?, target: Target<GifDrawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                // If GIF loads set to loop 3 times for now
                                resource?.setLoopCount(3)
                                return false
                            }
                        })
                        .into(holder.binding.projectImage)
                return
            }
        }
        // Otherwise load the static image for the project
        else {
            // Load the image
            loadImage(holder, photoUrl)
        }
    }

    private fun loadImage(holder: GalleryAdapterViewHolder, photoUrl: String) {
        Glide.with(holder.itemView.context)
                .load(File(photoUrl))
                .error(R.drawable.ic_sentiment_very_dissatisfied_white_24dp)
                .into(holder.binding.projectImage)
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

class ProjectViewDiffCallback : DiffUtil.ItemCallback<ProjectView>() {
    override fun areContentsTheSame(oldItem: ProjectView, newItem: ProjectView): Boolean {
        return oldItem.project_id == newItem.project_id
    }

    override fun areItemsTheSame(oldItem: ProjectView, newItem: ProjectView): Boolean {
        return oldItem == newItem
    }
}