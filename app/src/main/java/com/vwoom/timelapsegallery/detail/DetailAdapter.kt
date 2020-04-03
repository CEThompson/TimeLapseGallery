package com.vwoom.timelapsegallery.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.databinding.DetailRecyclerviewItemBinding
import com.vwoom.timelapsegallery.detail.DetailAdapter.DetailAdapterViewHolder
import com.vwoom.timelapsegallery.utils.ProjectUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils.getProjectEntryFromProjectView
import java.io.File

class DetailAdapter(private val mClickHandler: DetailAdapterOnClickHandler, val externalFilesDir: File) : RecyclerView.Adapter<DetailAdapterViewHolder>() {
    private var mPhotos: List<PhotoEntry> = emptyList()
    private lateinit var mProject: Project
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
        val currentPhoto = mPhotos[position]
        val photoPath = ProjectUtils.getProjectPhotoUrl(
                externalFilesDir,
                getProjectEntryFromProjectView(mProject),
                currentPhoto.timestamp)
        val f = File(photoPath)

        // TODO (update 1.2) dynamically resize detail view
        Glide.with(context)
                .load(f)
                .centerCrop()
                .into(binding.detailThumbnail)
        if (mPhotos[position] === mCurrentPhoto) {
            binding.selectionIndicator.visibility = View.VISIBLE
        } else {
            binding.selectionIndicator.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int {
        return mPhotos.size
    }

    fun setPhotoData(photoData: List<PhotoEntry>, project: Project) {
        mPhotos = photoData
        mProject = project
        notifyDataSetChanged()
    }

    fun setCurrentPhoto(photo: PhotoEntry) {
        mCurrentPhoto = photo
        notifyDataSetChanged()
    }
}