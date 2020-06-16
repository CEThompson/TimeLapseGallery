package com.vwoom.timelapsegallery.detail.dialog

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.detail.DetailViewModel
import java.io.File
import java.util.ArrayList

class ScheduleDialog(context: Context, val detailViewModel: DetailViewModel, externalFilesDir: File, var project: ProjectView): Dialog(context) {

    private val mNoneSelector: CardView
    private val mDaySelectionViews: ArrayList<CardView> = arrayListOf()
    private val mWeekSelectionViews: ArrayList<CardView> = arrayListOf()

    init {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setContentView(R.layout.dialog_schedule)
        this.setOnCancelListener { detailViewModel.scheduleDialogShowing = false }

        // Set up selector for no schedule
        val noneLayout = this.findViewById<FrameLayout>(R.id.dialog_schedule_none_layout)
        noneLayout.removeAllViews()
        mNoneSelector = layoutInflater.inflate(R.layout.dialog_schedule_selector, noneLayout, false) as CardView
        val noneTv = mNoneSelector.findViewById<TextView>(R.id.selector_child_tv)
        noneTv?.text = context.getString(R.string.unscheduled)
        mNoneSelector.contentDescription = context.getString(R.string.content_description_schedule_selector_none)
        noneLayout?.addView(mNoneSelector)
        mNoneSelector.setOnClickListener {
            detailViewModel.setSchedule(externalFilesDir, project, 0)

            // TODO: move analytics to view model?
            // TODO: reconsider logging schedule info
            //mFirebaseAnalytics?.logEvent(context.getString(R.string.analytics_delete_schedule), null)
        }

        // Set up days selection
        val daysLayout = this.findViewById<FlexboxLayout>(R.id.dialog_schedule_days_selection_layout)
        daysLayout.removeAllViews()
        for (dayInterval in 1..6) {
            // A selection layout for each interval
            val selectionLayout: CardView = layoutInflater.inflate(R.layout.dialog_schedule_selector, daysLayout, false) as CardView
            val textView = selectionLayout.findViewById<TextView>(R.id.selector_child_tv)
            textView.text = dayInterval.toString()
            selectionLayout.setOnClickListener {
                detailViewModel.setSchedule(externalFilesDir, project, dayInterval)
                // TODO: move analytics to view model?
                //mFirebaseAnalytics.logEvent(context.getString(R.string.analytics_add_schedule_days), null)
            }
            daysLayout?.addView(selectionLayout)
            selectionLayout.contentDescription = context.getString(R.string.content_description_schedule_selector_days, dayInterval)
            mDaySelectionViews.add(selectionLayout)
        }

        // Set up weeks selection
        val weeksLayout = this.findViewById<FlexboxLayout>(R.id.dialog_schedule_weeks_selection_layout)
        weeksLayout.removeAllViews()
        for (weekInterval in 1..4) {
            val selectionLayout: CardView = layoutInflater.inflate(R.layout.dialog_schedule_selector, daysLayout, false) as CardView
            val textView = selectionLayout.findViewById<TextView>(R.id.selector_child_tv)
            textView.text = weekInterval.toString()
            selectionLayout.setOnClickListener {
                detailViewModel.setSchedule(externalFilesDir, project, weekInterval * 7)
                // TODO: move analytics to view model?
                //mFirebaseAnalytics?.logEvent(context.getString(R.string.analytics_add_schedule_weeks), null)
            }
            weeksLayout?.addView(selectionLayout)
            selectionLayout.contentDescription = context.getString(R.string.content_description_schedule_selector_weeks, weekInterval)
            mWeekSelectionViews.add(selectionLayout)
        }

        // Set up custom input
        this.findViewById<EditText>(R.id.custom_schedule_input).addTextChangedListener {
            val interval = it.toString()
            if (interval.isNotEmpty()) {
                detailViewModel.setSchedule(externalFilesDir, project, interval.toInt())
                // TODO: reconsider analytics
                //mFirebaseAnalytics?.logEvent(getString(R.string.analytics_add_schedule_custom), null)
            } else {
                // TODO: reconsider analytics
                detailViewModel.setSchedule(externalFilesDir, project, 0)
                //mFirebaseAnalytics?.logEvent(getString(R.string.analytics_delete_schedule), null)
            }
        }

        // Set up dismissing the dialog
        val okTextView = this.findViewById<TextView>(R.id.dialog_schedule_dismiss)
        okTextView.setOnClickListener {
            this.dismiss()
            detailViewModel.scheduleDialogShowing = false
        }
        val exitFab = this.findViewById<FloatingActionButton>(R.id.schedule_dialog_exit_fab)
        exitFab.setOnClickListener {
            this.dismiss()
            detailViewModel.scheduleDialogShowing = false
        }

        // Update UI to current schedule
        setScheduleInformation(project)
    }

    // Updates UI of schedule dialog to current schedule
    fun setScheduleInformation(updatedProject: ProjectView) {
        project = updatedProject
        val colorSelected = R.color.colorPrimary
        val colorDefault = R.color.colorSubtleAccent
        val defaultElevation = 2f
        val selectedElevation = 6f

        val currentInterval = project.interval_days
        if (currentInterval == 0) {
            mNoneSelector.setCardBackgroundColor(ContextCompat.getColor(context, colorSelected))
            mNoneSelector.elevation = selectedElevation
        } else {
            mNoneSelector.setCardBackgroundColor(ContextCompat.getColor(context, colorDefault))
            mNoneSelector.elevation = defaultElevation
        }
        for (selector in mDaySelectionViews) {
            selector.setCardBackgroundColor(ContextCompat.getColor(context, colorDefault))
            selector.elevation = defaultElevation
            val selectorTv = selector.findViewById<TextView>(R.id.selector_child_tv)
            if (selectorTv.text == currentInterval.toString()) {
                selector.setCardBackgroundColor(ContextCompat.getColor(context, colorSelected))
                selector.elevation = selectedElevation
            }
        }
        for (selector in mWeekSelectionViews) {
            selector.setCardBackgroundColor(ContextCompat.getColor(context, colorDefault))
            selector.elevation = defaultElevation
            val selectorTv = selector.findViewById<TextView>(R.id.selector_child_tv)
            val currentWeekIntervalToDays = selectorTv.text.toString().toInt() * 7
            if (currentWeekIntervalToDays == currentInterval) {
                selector.setCardBackgroundColor(ContextCompat.getColor(context, colorSelected))
                selector.elevation = selectedElevation
            }
        }
        val scheduleOutput = this.findViewById<TextView>(R.id.dialog_schedule_result)
        if (project.interval_days == 0) scheduleOutput?.text = context.getString(R.string.none)
        else scheduleOutput?.text = context.getString(R.string.every_x_days, project.interval_days.toString())
    }

}