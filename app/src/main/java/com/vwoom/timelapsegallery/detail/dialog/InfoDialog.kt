package com.vwoom.timelapsegallery.detail.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.text.InputType
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.detail.DetailViewModel
import java.io.File

class InfoDialog(context: Context,
                 private val detailViewModel: DetailViewModel,
                 val externalFilesDir: File,
                 var project: ProjectView) : Dialog(context)
{
    init {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setContentView(R.layout.dialog_project_information)
        this.setOnCancelListener { detailViewModel.infoDialogShowing = false }
        // Get Views
        val editNameButton = this.findViewById<FloatingActionButton>(R.id.edit_project_name_button)
        // Set fab colors
        editNameButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
        editNameButton.setOnClickListener { verifyEditName() }
        val tagsTextView = this.findViewById<TextView>(R.id.dialog_information_tags)


        // TODO reconsider click listener on tags
        /*tagsTextView.setOnClickListener {
            if (mTagDialog == null) initializeTagDialog()
            this.show()
            detailViewModel.tagDialogShowing = true
        }*/

        val infoOkTextView = this.findViewById<TextView>(R.id.dialog_info_dismiss)
        infoOkTextView.setOnClickListener {
            this.dismiss()
            detailViewModel.infoDialogShowing = false
        }
        val exitFab = this.findViewById<FloatingActionButton>(R.id.project_info_exit_fab)
        exitFab.setOnClickListener {
            this.dismiss()
            detailViewModel.infoDialogShowing = false
        }

        // TODO figure out where to initialize dialog and tags
        //setInfoDialog()
        //setInfoTags()
    }

    // Verifies the user wishes to rename the project
    // This will rename the folder the images are written to.
    private fun verifyEditName() {
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.contentDescription = context.getString(R.string.content_description_edit_text_project_name)
        AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.edit_name))
                .setView(input)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    val nameText = input.text.toString().trim()
                    detailViewModel.updateProjectName(externalFilesDir, nameText, project)
                }
                .setNegativeButton(android.R.string.no, null).show()
    }

    // This updates the rest of the info in the project info dialog
    // Name, id, schedule, etc.
    // TODO: take updated project arg
    fun setInfoDialog(updatedProject: ProjectView) {
        project = updatedProject
        // Set info dialog fields
        // TODO move to init block?
        val projectInfoDialogId = this.findViewById<TextView>(R.id.dialog_project_info_id_field)
        projectInfoDialogId?.text = project.project_id.toString()

        val projectInfoNameTv = this.findViewById<TextView>(R.id.dialog_project_info_name)
        if (project.project_name == null || project.project_name!!.isEmpty()) {
            projectInfoNameTv.text = context.getString(R.string.unnamed)
        } else projectInfoNameTv.text = project.project_name
        if (project.interval_days == 0) {
            this.findViewById<TextView>(R.id.info_dialog_schedule_description).text = context.getString(R.string.none)
        } else {
            this.findViewById<TextView>(R.id.info_dialog_schedule_description).text =
                    context.getString(R.string.every_x_days, project.interval_days.toString())
        }
    }

    // This updates the tags in the project info dialog
    fun setInfoTags(text: String) {
        val tagsTextView = this.findViewById<TextView>(R.id.dialog_information_tags)
        tagsTextView.text = text
    }
}