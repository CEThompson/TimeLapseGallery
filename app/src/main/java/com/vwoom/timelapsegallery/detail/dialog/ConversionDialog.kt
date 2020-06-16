package com.vwoom.timelapsegallery.detail.dialog

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
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

// TODO: handle gif sharing
// TODO: add relevant tests for ffmpeg branch
class ConversionDialog(context: Context, detailViewModel: DetailViewModel, externalFilesDir: File, project: ProjectView) : Dialog(context) {
    init {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setContentView(R.layout.dialog_project_conversion)
        this.setOnCancelListener { detailViewModel.convertDialogShowing = false }

        // TODO: handle vertical / horizontal modes
        // Constrain dialog size
        val dm = context.resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels
        val newWidth = (width * 0.7).toInt()
        val newHeight = (height * 0.7).toInt()
        this.findViewById<ConstraintLayout>(R.id.conversion_dialog_layout).layoutParams.width = newWidth
        this.findViewById<ConstraintLayout>(R.id.conversion_dialog_layout).layoutParams.height = newHeight

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

    }

    private fun deleteGif(externalFilesDir: File, project: ProjectView) {
        val curGif = ProjectUtils.getGifForProject(externalFilesDir, ProjectUtils.getProjectEntryFromProjectView(project))
        if (curGif != null) FileUtils.deleteRecursive(curGif)
    }


}