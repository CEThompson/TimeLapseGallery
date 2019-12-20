package com.vwoom.timelapsegallery.adapters

import android.content.Context
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.bumptech.glide.Glide
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.adapters.DetailsAdapter
import com.vwoom.timelapsegallery.adapters.DetailsAdapter.DetailsAdapterViewHolder
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.utils.FileUtils
import java.io.File

class DetailsAdapter(private val mClickHandler: DetailsAdapterOnClickHandler, context: Context) : RecyclerView.Adapter<DetailsAdapterViewHolder>() {
    private var mPhotos: List<PhotoEntry>? = null
    private var mProject: Project? = null
    private var mCurrentPhoto: PhotoEntry? = null
    private val mExternalFilesDir: File?

    interface DetailsAdapterOnClickHandler {
        fun onClick(clickedPhoto: PhotoEntry?)
    }

    inner class DetailsAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        @BindView(R.id.detail_thumbnail)
        var mDetailThumbnail: ImageView? = null
        @BindView(R.id.selection_indicator)
        var mSelectionIndicator: View? = null

        override fun onClick(view: View) {
            val adapterPosition = adapterPosition
            val clickedPhoto = mPhotos!![adapterPosition]
            mCurrentPhoto = clickedPhoto
            mClickHandler.onClick(clickedPhoto)
        }

        init {
            ButterKnife.bind(this, view)
            view.setOnClickListener(this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailsAdapterViewHolder {
        val context = parent.context
        val layoutIdForGridItem = R.layout.detail_recyclerview_item
        val inflater = LayoutInflater.from(context)
        val shouldAttachToParentImmediately = false
        val view = inflater.inflate(layoutIdForGridItem, parent, shouldAttachToParentImmediately)
        return DetailsAdapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: DetailsAdapterViewHolder, position: Int) {
        val context = holder.itemView.context
        val currentPhoto = mPhotos!![position]
        val photo_path = FileUtils.getPhotoUrl(mExternalFilesDir, mProject, currentPhoto)
        val f = File(photo_path)
        // TODO (update) dynamically resize detail view
        Glide.with(context)
                .load(f)
                .centerCrop()
                .into(holder.mDetailThumbnail!!)
        if (mPhotos!![position] === mCurrentPhoto) {
            holder.mSelectionIndicator!!.visibility = View.VISIBLE
        } else {
            holder.mSelectionIndicator!!.visibility = View.INVISIBLE
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