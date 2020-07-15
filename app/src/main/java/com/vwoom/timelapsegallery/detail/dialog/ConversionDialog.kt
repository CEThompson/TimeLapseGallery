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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.detail.DetailViewModel
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// TODO (1.2): review ffmpeg / gif feature tests
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
            this.findViewById<ConstraintLayout>(R.id.conversion_dialog_layout).layoutParams.height = newHeight
            this.findViewById<ConstraintLayout>(R.id.conversion_dialog_layout).layoutParams.width = newWidth
        }
        // If landscape just set by height (width auto adjusts for different layout)
        else {
            val newHeight = (height * 0.85).toInt()
            this.findViewById<ConstraintLayout>(R.id.conversion_dialog_layout).layoutParams.height = newHeight
        }

        // Initialize the gif preview
        val gifFile = ProjectUtils.getGifForProject(externalFilesDir, ProjectUtils.getProjectEntryFromProjectView(project))
        val gifPreview = this.findViewById<ImageView>(R.id.dialog_conversion_gif_preview)
        if (gifPreview != null && gifFile != null)
            Glide.with(context)
                    .load(gifFile)
                    .fitCenter()
                    .into(gifPreview)

        // Convert the project photos to gif on click
        val convertFab = this.findViewById<FloatingActionButton>(R.id.dialog_project_conversion_convert_FAB)
        convertFab?.setOnClickListener {
            this.findViewById<ProgressBar>(R.id.conversion_progress)?.visibility = View.VISIBLE
            // Launch the conversion on IO thread
            val convertJob = detailViewModel.viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    // First delete the old GIF
                    deleteGif(externalFilesDir, project)
                    // Then write the new one
                    ProjectUtils.makeGif(externalFilesDir, ProjectUtils.getProjectEntryFromProjectView(project))
                }
            }
            // On completion update the preview
            convertJob.invokeOnCompletion {
                val updatedGif = ProjectUtils.getGifForProject(externalFilesDir, ProjectUtils.getProjectEntryFromProjectView(project))
                this.findViewById<ProgressBar>(R.id.conversion_progress)?.visibility = View.INVISIBLE
                if (gifPreview != null && updatedGif != null)
                    Glide.with(context)
                            .load(updatedGif)
                            .fitCenter()
                            .into(gifPreview)
            }
        }

        // Delete the gif for the project
        val delFab = this.findViewById<FloatingActionButton>(R.id.dialog_project_conversion_remove_FAB)
        delFab.setOnClickListener {
            deleteGif(externalFilesDir, project)
            if (gifPreview != null)
                Glide.with(context)
                        .load(R.color.imagePlaceholder)
                        .fitCenter()
                        .into(gifPreview)
        }

        val exitFab = this.findViewById<FloatingActionButton>(R.id.dialog_conversion_exit_fab)
        exitFab.setOnClickListener {
            this.dismiss()
            detailViewModel.convertDialogShowing = false
        }

        val shareFab = this.findViewById<FloatingActionButton>(R.id.dialog_conversion_share_FAB)
        shareFab.setOnClickListener {
            val gif = ProjectUtils.getGifForProject(externalFilesDir, ProjectUtils.getProjectEntryFromProjectView(project))
            if (gif != null) {
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/gif"
                    val photoURI: Uri = FileProvider.getUriForFile(context,
                            context.applicationContext.packageName.toString() + ".fileprovider",
                            gif)
                    putExtra(Intent.EXTRA_STREAM, photoURI)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
            }
            // Gif has not yet been created
            else {
                Toast.makeText(context, "Create a GIF first to share", Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun deleteGif(externalFilesDir: File, project: ProjectView) {
        val curGif = ProjectUtils.getGifForProject(externalFilesDir, ProjectUtils.getProjectEntryFromProjectView(project))
        if (curGif != null) FileUtils.deleteRecursive(curGif)
    }


}