package com.vwoom.timelapsegallery.detail.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.detail.DetailViewModel

class TagDialog(context: Context, private val detailViewModel: DetailViewModel, val project: ProjectView) : Dialog(context) {
    init {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setContentView(R.layout.dialog_project_tag)
        this.setOnCancelListener { detailViewModel.tagDialogShowing = false }
        // set add tag fab
        val editText = this.findViewById<EditText>(R.id.add_tag_dialog_edit_text)
        val addTagFab = this.findViewById<FloatingActionButton>(R.id.add_tag_fab)
        addTagFab.setOnClickListener {
            val tagText = editText?.text.toString().trim()
            when {
                tagText.isEmpty() -> {
                    return@setOnClickListener
                }
                tagText.contains(' ') -> showTagValidationAlertDialog(context.getString(R.string.invalid_tag_one_word))
                tagText.length > 14 -> showTagValidationAlertDialog(context.getString(R.string.invalid_tag_length))
                else -> {
                    detailViewModel.addTag(tagText, project)
                    editText?.text?.clear()
                }
            }
        }
        val dismissView = this.findViewById<TextView>(R.id.dialog_project_tag_dismiss)
        dismissView.setOnClickListener {
            this.dismiss()
            detailViewModel.tagDialogShowing = false
        }
        val exitFab = this.findViewById<FloatingActionButton>(R.id.project_tag_exit_fab)
        exitFab.setOnClickListener {
            this.dismiss()
            detailViewModel.tagDialogShowing = false
        }
    }

    // Gives user feedback on tags
    private fun showTagValidationAlertDialog(message: String) {
        AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.invalid_tag))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _: Int ->
                }.show()
    }

    // This sets the tags in the project info dialog
    // This creates text views for all tags in the database, but if the tags
    // belong to the project then they are styled appropriately for user feedback
    fun setProjectTagDialog(allTags: List<TagEntry>, projectTags: List<TagEntry>) {

        val availableTagsLayout = this.findViewById<FlexboxLayout>(R.id.project_tag_dialog_available_tags_layout)
        availableTagsLayout?.removeAllViews()
        // Set up the available tags in the project information dialog
        val instructionTv = this.findViewById<TextView>(R.id.tag_deletion_instructions)
        if (allTags.isEmpty()) {
            instructionTv?.text = context.getString(R.string.tag_start_instruction)
        } else {
            instructionTv?.text = context.getString(R.string.tag_deletion_instruction)
            // Add the tags to the layout
            for (tagEntry in allTags) {
                // Inflate the tag and set its text
                val textView: TextView = layoutInflater.inflate(R.layout.tag_text_view, availableTagsLayout, false) as TextView
                textView.text = context.getString(R.string.hashtag, tagEntry.text)

                // Style depending upon whether or not this particular project is tagged
                val tagInProject: Boolean = projectTags.contains(tagEntry)
                if (tagInProject) {
                    textView.setTextColor(ContextCompat.getColor(context, R.color.colorTag))
                    textView.setOnClickListener { detailViewModel.deleteTagFromProject(tagEntry, project) }
                } else {
                    textView.setTextColor(ContextCompat.getColor(context, R.color.grey))
                    textView.setOnClickListener { detailViewModel.addTag(tagEntry.text, project) }
                }

                // Set tag deletion
                textView.setOnLongClickListener {
                    verifyTagDeletion(tagEntry)
                    true
                }

                // Add the view to the flex box layout
                availableTagsLayout?.addView(textView)
            }
        }
    }


    // Ensures the user wishes to delete the selected tag
    private fun verifyTagDeletion(tagEntry: TagEntry) {
        AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.delete_tag, tagEntry.text))
                .setMessage(context.getString(R.string.verify_delete_tag, tagEntry.text))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok) { _, _: Int ->
                    // If this photo is the last photo then set the new thumbnail to its previous
                    detailViewModel.deleteTagFromDatabase(tagEntry)
                }
                .setNegativeButton(android.R.string.cancel, null).show()
    }

}