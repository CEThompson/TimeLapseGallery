package com.vwoom.timelapsegallery.gallery

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.Window
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.entry.TagEntry
import java.util.*

// TODO: consider implementing search tag filters as chips instead of checkboxes
class SearchDialog(context: Context, private val galleryViewModel: GalleryViewModel) : Dialog(context) {

    init {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setContentView(R.layout.dialog_search)

        this.setOnCancelListener { galleryViewModel.searchDialogShowing = false }

        val searchEditText = this.findViewById<EditText>(R.id.search_edit_text)
        val exitFab = this.findViewById<FloatingActionButton>(R.id.search_dialog_exit_fab)
        val okDismiss = this.findViewById<TextView>(R.id.search_dialog_dismiss)
        exitFab?.setOnClickListener { this.cancel() }
        okDismiss?.setOnClickListener { this.cancel() }

        val dueTodayCheckBox = this.findViewById<CheckBox>(R.id.search_due_today_checkbox)
        val dueTomorrowCheckBox = this.findViewById<CheckBox>(R.id.search_due_tomorrow_checkbox)
        val pendingCheckBox = this.findViewById<CheckBox>(R.id.search_pending_checkbox)
        val scheduledCheckBox = this.findViewById<CheckBox>(R.id.search_scheduled_checkbox)
        val unscheduledCheckBox = this.findViewById<CheckBox>(R.id.search_unscheduled_checkbox)

        searchEditText?.setText(galleryViewModel.searchName)   // recover current search term

        searchEditText?.addTextChangedListener {
            val searchName = it.toString().trim()
            galleryViewModel.searchName = searchName
            updateSearchFilter()
        }

        // Handle search selection of scheduled / unscheduled projects
        dueTodayCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) galleryViewModel.searchType = SearchType.DueToday
            else galleryViewModel.searchType = SearchType.None
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        dueTomorrowCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) galleryViewModel.searchType = SearchType.DueTomorrow
            else galleryViewModel.searchType = SearchType.None
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        pendingCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) galleryViewModel.searchType = SearchType.Pending
            else galleryViewModel.searchType = SearchType.None
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        scheduledCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) galleryViewModel.searchType = SearchType.Scheduled
            else galleryViewModel.searchType = SearchType.None
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        unscheduledCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) galleryViewModel.searchType = SearchType.Unscheduled
            else galleryViewModel.searchType = SearchType.None
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        updateSearchDialog()
    }

    // Updates the dialog with all tags in the database for filtration
    // And updates the state of the checkboxes in the dialog
    fun updateSearchDialog() {
        // 1. Update the name edit text
        this.findViewById<EditText>(R.id.search_edit_text)?.setText(galleryViewModel.searchName)

        // 2. Update tag state
        updateSearchDialogTags()

        // 3. Update the the checkboxes
        updateSearchDialogCheckboxes()
    }

    // This sets the tags
    private fun updateSearchDialogTags() {
        var tags: List<TagEntry> = listOf()
        if (galleryViewModel.tags.value != null) {
            tags = galleryViewModel.tags.value!!.sortedBy { it.text.toLowerCase(Locale.getDefault()) }
        }
        // Clear the tag layout
        val tagLayout = this.findViewById<FlexboxLayout>(R.id.dialog_search_tags_layout)
        val emptyListIndicator = this.findViewById<TextView>(R.id.empty_tags_label)
        tagLayout?.removeAllViews()
        // Show no tag indicator
        if (tags.isEmpty()) {
            emptyListIndicator?.visibility = View.VISIBLE
            tagLayout?.visibility = View.GONE
        } else {
            tagLayout?.visibility = View.VISIBLE
            emptyListIndicator?.visibility = View.GONE
        }
        // Create the tag views
        for (tag in tags) {
            val tagCheckBox = CheckBox(context)
            tagCheckBox.text = context.getString(R.string.hashtag, tag.text)
            tagCheckBox.setTypeface(null, Typeface.ITALIC)
            tagCheckBox.alpha = .8f
            tagCheckBox.setTextColor(ContextCompat.getColor(context, R.color.colorTag))
            tagCheckBox.isChecked = galleryViewModel.tagSelected(tag)
            tagCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) galleryViewModel.searchTags.add(tag)
                else galleryViewModel.searchTags.remove(tag)
                updateSearchFilter()
            }
            tagLayout?.addView(tagCheckBox)
        }
    }

    // This sets the checkboxes
    private fun updateSearchDialogCheckboxes() {
        // Update the state of search by schedule layout
        val dueTodayCheckBox = this.findViewById<CheckBox>(R.id.search_due_today_checkbox)
        val dueTomorrowCheckBox = this.findViewById<CheckBox>(R.id.search_due_tomorrow_checkbox)
        val pendingCheckBox = this.findViewById<CheckBox>(R.id.search_pending_checkbox)
        val scheduledCheckBox = this.findViewById<CheckBox>(R.id.search_scheduled_checkbox)
        val unscheduledCheckBox = this.findViewById<CheckBox>(R.id.search_unscheduled_checkbox)
        dueTodayCheckBox?.isChecked = galleryViewModel.searchType == SearchType.DueToday
        dueTomorrowCheckBox?.isChecked = galleryViewModel.searchType == SearchType.DueTomorrow
        pendingCheckBox?.isChecked = galleryViewModel.searchType == SearchType.Pending
        scheduledCheckBox?.isChecked = galleryViewModel.searchType == SearchType.Scheduled
        unscheduledCheckBox?.isChecked = galleryViewModel.searchType == SearchType.Unscheduled
    }

    private fun updateSearchFilter() {
        galleryViewModel.filterProjects()
        galleryViewModel.setSearch()
    }

}