package com.vwoom.timelapsegallery.detail.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.detail.DetailViewModel
import com.vwoom.timelapsegallery.gif.GifUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class ConversionDialog(context: Context,
                       detailViewModel: DetailViewModel,
                       externalFilesDir: File,
                       project: ProjectView) : Dialog(context) {
    init {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setContentView(R.layout.dialog_project_conversion)
        this.setOnCancelListener { detailViewModel.convertDialogShowing = false }

        // Constrain dialog size
        val dm = context.resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels

        val orientation = this.context.resources.configuration.orientation
        // If portrait set new width and height
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            val newWidth = (width * 0.7).toInt()
            val newHeight = (height * 0.7).toInt()
            val conversionDialogLayout = this.findViewById<ConstraintLayout>(R.id.conversion_dialog_layout)
            conversionDialogLayout.layoutParams.height = newHeight
            conversionDialogLayout.layoutParams.width = newWidth
        }
        // If landscape just set by height (width auto adjusts for different layout)
        else {
            val newHeight = (height * 0.85).toInt()
            this.findViewById<ConstraintLayout>(R.id.conversion_dialog_layout).layoutParams.height = newHeight
        }

        // Initialize the gif preview
        val gifFile = GifUtils.getGifForProject(externalFilesDir, ProjectUtils.getProjectEntryFromProjectView(project))
        val gifPreview = this.findViewById<ImageView>(R.id.dialog_conversion_gif_preview)
        if (gifPreview != null && gifFile != null)
            loadGif(context, gifFile, gifPreview)

        // Convert the project photos to gif on click
        val convertFab = this.findViewById<FloatingActionButton>(R.id.dialog_project_conversion_convert_FAB)
        convertFab?.setOnClickListener {
            // Disable the button
            val button = it
            button.isEnabled = false

            // Show the progress indicator and hide the old GIF
            this.findViewById<ProgressBar>(R.id.conversion_progress)?.visibility = View.VISIBLE
            gifPreview.setImageResource(R.color.imagePlaceholder)

            // Launch the conversion in a job
            val convertJob = detailViewModel.viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    GifUtils.updateGif(externalFilesDir, ProjectUtils.getProjectEntryFromProjectView(project))
                }
            }
            // On completion update the preview
            convertJob.invokeOnCompletion {
                val updatedGif = GifUtils.getGifForProject(externalFilesDir, ProjectUtils.getProjectEntryFromProjectView(project))
                this.findViewById<ProgressBar>(R.id.conversion_progress)?.visibility = View.INVISIBLE
                if (gifPreview != null && updatedGif != null)
                    loadGif(context, updatedGif, gifPreview)
                button.isEnabled = true
            }
        }

        // Delete the gif for the project
        val delFab = this.findViewById<FloatingActionButton>(R.id.dialog_project_conversion_remove_FAB)
        delFab.setOnClickListener {
            gifPreview.setImageResource(R.color.imagePlaceholder)
            detailViewModel.viewModelScope.launch {
                GifUtils.deleteGif(externalFilesDir, ProjectUtils.getProjectEntryFromProjectView(project))
            }
        }

        val exitFab = this.findViewById<FloatingActionButton>(R.id.dialog_conversion_exit_fab)
        exitFab.setOnClickListener {
            this.dismiss()
            detailViewModel.convertDialogShowing = false
        }

        val shareFab = this.findViewById<FloatingActionButton>(R.id.dialog_conversion_share_FAB)
        shareFab.setOnClickListener {
            val gif = GifUtils.getGifForProject(externalFilesDir, ProjectUtils.getProjectEntryFromProjectView(project))
            if (gif != null) {
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/gif"
                    val photoURI: Uri = FileProvider.getUriForFile(context,
                            context.applicationContext.packageName.toString() + ".fileprovider",
                            gif)
                    putExtra(Intent.EXTRA_STREAM, photoURI)
                }
                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_gif_title)))
            }
            // Gif has not yet been created
            else {
                Toast.makeText(context, context.getString(R.string.create_gif_to_share), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadGif(context: Context, gifFile: File, gifPreview: ImageView) {
        Glide.with(context)
                .asGif()
                .load(gifFile)
                //.signature(MediaStoreSignature(null, null, null))
                //.apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
                .listener(object : RequestListener<GifDrawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<GifDrawable>?, isFirstResource: Boolean): Boolean {
                        Timber.d(e)
                        return false
                    }

                    override fun onResourceReady(resource: GifDrawable?, model: Any?, target: Target<GifDrawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        return false
                    }
                })
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .skipMemoryCache(true)
                .fitCenter()
                .into(gifPreview)
    }

}