package com.vwoom.timelapsegallery.details

import android.content.Context
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.details.DetailsAdapter.DetailsAdapterViewHolder
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.databinding.DetailRecyclerviewItemBinding
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.utils.FileUtils
import kotlinx.android.synthetic.main.detail_recyclerview_item.view.*
import java.io.File

class DetailsAdapter(private val mClickHandler: DetailsAdapterOnClickHandler, context: Context) : RecyclerView.Adapter<DetailsAdapterViewHolder>() {
    private var mPhotos: List<PhotoEntry>? = null
    private var mProject: Project? = null
    private var mCurrentPhoto: PhotoEntry? = null
    private val mExternalFilesDir: File?

    interface DetailsAdapterOnClickHandler {
        fun onClick(clickedPhoto: PhotoEntry)
    }

    inner class DetailsAdapterViewHolder(var binding: DetailRecyclerviewItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        override fun onClick(view: View) {
            val adapterPosition = adapterPosition
            val clickedPhoto = mPhotos!![adapterPosition]
            mCurrentPhoto = clickedPhoto
            mClickHandler.onClick(clickedPhoto)
        }

        init {
            binding.root.setOnClickListener(this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailsAdapterViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val shouldAttachToParentImmediately = false
        val binding = DetailRecyclerviewItemBinding.inflate(inflater, parent, shouldAttachToParentImmediately)
        return DetailsAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailsAdapterViewHolder, position: Int) {
        val binding = holder.binding
        val context = holder.itemView.context
        val currentPhoto = mPhotos?.get(position)
        val photo_path = FileUtils.getPhotoUrl(mExternalFilesDir!!, mProject!!, currentPhoto!!)
        val f = File(photo_path)
        // TODO (update) dynamically resize detail view
        Glide.with(context)
                .load(f)
                .centerCrop()
                .into(binding.detailThumbnail)
        if (mPhotos!![position] === mCurrentPhoto) {
            binding.selectionIndicator.visibility = View.VISIBLE
        } else {
            binding.selectionIndicator.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int {
        return if (mPhotos == null) 0 else mPhotos!!.size
    }

    fun setPhotoData(photoData: List<PhotoEntry>?, project: Project?) {
        mPhotos = photoData
        mProject = project
        notifyDataSetChanged()
    }

    fun setCurrentPhoto(photo: PhotoEntry?) {
        mCurrentPhoto = photo
        notifyDataSetChanged()
    }

    companion object {
        private val TAG = DetailsAdapter::class.java.simpleName
    }

    init {
        mExternalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    }
}