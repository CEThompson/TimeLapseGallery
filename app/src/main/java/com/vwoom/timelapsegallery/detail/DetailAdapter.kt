package com.vwoom.timelapsegallery.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.databinding.DetailRecyclerviewItemBinding
import com.vwoom.timelapsegallery.detail.DetailAdapter.DetailAdapterViewHolder
import com.vwoom.timelapsegallery.utils.ProjectUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils.getProjectEntryFromProjectView
import java.io.File

class DetailAdapter(private val mClickHandler: DetailAdapterOnClickHandler, val externalFilesDir: File) : RecyclerView.Adapter<DetailAdapterViewHolder>() {
    private var mPhotos: List<PhotoEntry> = emptyList()
    private lateinit var mProjectView: ProjectView
    private lateinit var mCurrentPhoto: PhotoEntry

    interface DetailAdapterOnClickHandler {
        fun onClick(clickedPhoto: PhotoEntry)
    }

    inner class DetailAdapterViewHolder(var binding: DetailRecyclerviewItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        override fun onClick(view: View) {
            val adapterPosition = adapterPosition
            val clickedPhoto = mPhotos[adapterPosition]
            mCurrentPhoto = clickedPhoto
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
        val currentPhoto: PhotoEntry = mPhotos[position]
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

        if (mPhotos[position] === mCurrentPhoto) {
            binding.selectionIndicator.visibility = View.VISIBLE
        } else {
            binding.selectionIndicator.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int {
        return mPhotos.size
    }

    fun setPhotoData(photoData: List<PhotoEntry>, projectView: ProjectView) {
        mPhotos = photoData
        mProjectView = projectView
        notifyDataSetChanged()
    }

    fun setCurrentPhoto(photo: PhotoEntry) {
        mCurrentPhoto = photo
        notifyDataSetChanged()
    }
}