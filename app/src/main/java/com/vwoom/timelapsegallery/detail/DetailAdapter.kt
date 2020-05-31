package com.vwoom.timelapsegallery.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.databinding.DetailRecyclerviewItemBinding
import com.vwoom.timelapsegallery.detail.DetailAdapter.DetailAdapterViewHolder
import com.vwoom.timelapsegallery.utils.ProjectUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils.getProjectEntryFromProjectView
import java.io.File

class DetailAdapter(private val mClickHandler: DetailAdapterOnClickHandler, val externalFilesDir: File)
    : ListAdapter<PhotoEntry, DetailAdapterViewHolder>(PhotoDiffCallback()) {

    private lateinit var mProjectView: ProjectView
    private lateinit var mCurrentPhoto: PhotoEntry

    interface DetailAdapterOnClickHandler {
        fun onClick(clickedPhoto: PhotoEntry)
    }

    inner class DetailAdapterViewHolder(var binding: DetailRecyclerviewItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        override fun onClick(view: View) {
            val adapterPosition = adapterPosition
            val clickedPhoto = getItem(adapterPosition)
            //mCurrentPhoto = clickedPhoto
            setCurrentPhoto(clickedPhoto)
            mClickHandler.onClick(clickedPhoto)
        }

        init {
            binding.root.setOnClickListener(this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailAdapterViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val shouldAttachToParentImmediately = false
        val binding = DetailRecyclerviewItemBinding.inflate(inflater, parent, shouldAttachToParentImmediately)
        return DetailAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailAdapterViewHolder, position: Int) {
        val binding = holder.binding
        val context = holder.itemView.context
        val currentPhoto: PhotoEntry = getItem(position)

        val photoPath: String? = ProjectUtils.getProjectPhotoUrl(
                externalFilesDir,
                getProjectEntryFromProjectView(mProjectView),
                currentPhoto.timestamp)

        // Otherwise continue on to load the correct image
        val f = if (photoPath == null) null else File(photoPath)
        // TODO (update 1.2) dynamically resize detail view

        if (f == null){
            Glide.with(context)
                    .load(R.drawable.ic_sentiment_very_dissatisfied_white_24dp)
                    .centerInside()
                    .into(binding.detailThumbnail)
        } else {
            Glide.with(context)
                    .load(f)
                    .error(R.drawable.ic_sentiment_very_dissatisfied_white_24dp)
                    .centerCrop()
                    .into(binding.detailThumbnail)
        }

        if (currentPhoto == mCurrentPhoto) {
            binding.selectionIndicator.visibility = View.VISIBLE
        } else {
            binding.selectionIndicator.visibility = View.INVISIBLE
        }
    }

    fun setProject(projectView: ProjectView) {
        mProjectView = projectView
    }

    fun setCurrentPhoto(photo: PhotoEntry) {
        if (!this::mCurrentPhoto.isInitialized) {
            mCurrentPhoto = photo
            return
        } else {
            val previous = mCurrentPhoto
            mCurrentPhoto = photo
            val prevPos = this.currentList.indexOf(previous)
            val currentPos = this.currentList.indexOf(photo)
            this.notifyItemChanged(prevPos)
            this.notifyItemChanged(currentPos)
        }
        /*mCurrentPhoto = photo
        notifyDataSetChanged()*/
    }
}

class PhotoDiffCallback: DiffUtil.ItemCallback<PhotoEntry>() {
    override fun areContentsTheSame(oldItem: PhotoEntry, newItem: PhotoEntry): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areItemsTheSame(oldItem: PhotoEntry, newItem: PhotoEntry): Boolean {
        return oldItem == newItem
    }
}