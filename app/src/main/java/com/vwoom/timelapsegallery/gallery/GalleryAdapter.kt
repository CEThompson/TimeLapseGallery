package com.vwoom.timelapsegallery.gallery

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.gallery.GalleryAdapter.GalleryAdapterViewHolder
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.PhotoUtils
import java.io.File
import java.util.*

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

        // Display a check if a picture was taken today
        val photoTakenToday = DateUtils.isToday(project.cover_photo_timestamp)
        if (photoTakenToday){
            holder.binding.scheduleIndicatorCheck.visibility = View.VISIBLE
        } else {
            holder.binding.scheduleIndicatorCheck.visibility = View.GONE
        }

        // Display if the project is scheduled or not
        if (project.interval_days != null && project.interval_days != 0) {
            // Display the correctly colored schedule indicator
            if (photoTakenToday) {
                // Green if a photo was taken today
                holder.binding.scheduleIndicatorPending.visibility = View.VISIBLE
                holder.binding.scheduleIndicatorDue.visibility = View.GONE
            }
            else {
                // Red if a photo needs to be taken today
                holder.binding.scheduleIndicatorDue.visibility = View.VISIBLE
                holder.binding.scheduleIndicatorPending.visibility = View.GONE
            }
            // Display the gradient for readability
            holder.binding.projectImageGradient.visibility = View.VISIBLE
        }
        else {
            holder.binding.scheduleIndicatorDue.visibility = View.GONE
            holder.binding.scheduleIndicatorPending.visibility = View.GONE
            holder.binding.projectImageGradient.visibility = View.GONE
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

    fun setProjectData(projectData: List<Project>) {
        mProjectData = projectData

        // TODO convert this to a diff util?
        mProjectsToCoverPhotos.clear()
        for (project in projectData){
            val photoUrl = FileUtils.getCoverPhotoUrl(externalFilesDir, project)
            val ratio = PhotoUtils.getAspectRatioFromImagePath(photoUrl)
            val file = File(photoUrl)
            mProjectsToCoverPhotos.apply {put(project, file)}
            mCoverPhotosToRatios.apply{put(file, ratio)}
        }

        notifyDataSetChanged()
    }

    companion object {
        private val TAG = GalleryAdapter::class.java.simpleName
    }
}