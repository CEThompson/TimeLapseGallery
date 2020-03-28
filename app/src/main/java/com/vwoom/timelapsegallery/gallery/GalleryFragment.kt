package com.vwoom.timelapsegallery.gallery

import android.app.Dialog
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.os.SystemClock
import android.transition.Transition
import android.transition.TransitionInflater
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.databinding.FragmentGalleryBinding
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.utils.InjectorUtils
import com.vwoom.timelapsegallery.utils.PhotoUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class GalleryFragment : Fragment(), GalleryAdapter.GalleryAdapterOnClickHandler {

    private var mSearchDialog: Dialog? = null

    private var mGalleryAdapter: GalleryAdapter? = null
    private var mGridLayoutManager: StaggeredGridLayoutManager? = null

    private var searchJob: Job? = null

    private val args: GalleryFragmentArgs by navArgs()

    private var mGalleryRecyclerView: RecyclerView? = null
    private var mAddProjectFAB: FloatingActionButton? = null
    private var mSearchActiveFAB: FloatingActionButton? = null
    private var toolbar: Toolbar? = null

    private var binding: FragmentGalleryBinding? = null

    private val mGalleryViewModel: GalleryViewModel by viewModels {
        InjectorUtils.provideGalleryViewModelFactory(requireActivity())
    }

    private var mPrevProjectsSize: Int? = null

    // prevent double clicking
    private var mLastClickTime: Long? = null

    override fun onPause() {
        super.onPause()
        mSearchDialog?.dismiss()
    }

    override fun onStop() {
        super.onStop()
        searchJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mGalleryRecyclerView = null
        mAddProjectFAB = null
        mSearchActiveFAB = null
        mGridLayoutManager = null
        mGalleryAdapter = null
        toolbar = null
        binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reenter = TransitionInflater.from(context).inflateTransition(R.transition.gallery_exit_transition)
        reenter.addListener(object : Transition.TransitionListener {
            override fun onTransitionEnd(transition: Transition?) {
            }

            override fun onTransitionCancel(transition: Transition?) {
            }

            override fun onTransitionStart(transition: Transition?) {
                val fadeInAnimation = AlphaAnimation(0f, 1f)
                fadeInAnimation.duration = 375
                binding?.galleryRecyclerView?.startAnimation(fadeInAnimation)
            }

            override fun onTransitionPause(transition: Transition?) {
            }

            override fun onTransitionResume(transition: Transition?) {
            }
        })
        exitTransition = TransitionInflater.from(context).inflateTransition(R.transition.gallery_exit_transition)
        reenterTransition = reenter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentGalleryBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

        // Set up options menu
        setHasOptionsMenu(true)
        toolbar = binding?.galleryFragmentToolbar
        (activity as TimeLapseGalleryActivity).setSupportActionBar(toolbar)
        toolbar?.title = getString(R.string.app_name)
        (activity as TimeLapseGalleryActivity).supportActionBar?.setIcon(R.drawable.actionbar_space_between_icon_and_title)

        // Increase columns for horizontal orientation
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> mNumberOfColumns = 6
            Configuration.ORIENTATION_PORTRAIT -> mNumberOfColumns = 3
        }

        // Set up the adapter for the recycler view
        val externalFilesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        mGalleryAdapter = GalleryAdapter(this, externalFilesDir!!)

        // Set up the recycler view
        mGridLayoutManager = StaggeredGridLayoutManager(mNumberOfColumns, StaggeredGridLayoutManager.VERTICAL)
        mGalleryRecyclerView = binding?.galleryRecyclerView
        mGalleryRecyclerView?.apply {
            layoutManager = mGridLayoutManager
            setHasFixedSize(false)
            adapter = mGalleryAdapter
            postponeEnterTransition()
        }

        // TODO: (update 1.2) re-evaluate transition after taking pictures of a project, filtered projects do not update immediately
        if (mGalleryViewModel.displayedProjects.isNotEmpty())
            mGalleryAdapter?.setProjectData(mGalleryViewModel.displayedProjects)

        mGalleryRecyclerView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                startPostponedEnterTransition()
                mGalleryRecyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
            }
        })

        // Set up navigation to add new projects
        mAddProjectFAB = binding?.addProjectFAB
        mAddProjectFAB?.setOnClickListener {
            exitTransition = null // When navigating to the camera skip the fade out animation
            reenterTransition = null // also skip fade in on return
            val cameraId = PhotoUtils.findCamera(requireContext())
            if (cameraId == null){
                Toast.makeText(requireContext(), getString(R.string.no_camera_found), Toast.LENGTH_LONG).show()
            } else {
                val action = GalleryFragmentDirections.actionGalleryFragmentToCamera2Fragment(cameraId, null, null)
                findNavController().navigate(action)
            }
        }

        mSearchActiveFAB = binding?.searchActiveIndicator
        mSearchActiveFAB?.setOnClickListener {
            mGalleryViewModel.userClickedToStopSearch = true
            clearSearch()
        }

        setupViewModel()

        // Launch with search filter if set from the notification
        if (args.searchLaunchDue && !mGalleryViewModel.userClickedToStopSearch) {
            mGalleryViewModel.searchType = SEARCH_TYPE_DUE_TODAY
        }

        // Hide or show search cancel fab depending upon whether or not the user is searching
        if (!userIsNotSearching()) mSearchActiveFAB?.show()
        else mSearchActiveFAB?.hide()

        return binding?.root
    }

    private fun clearSearch() {
        // Clear search
        mGalleryViewModel.searchName = ""
        mGalleryViewModel.searchTags.clear()
        mGalleryViewModel.searchType = SEARCH_TYPE_NONE
        updateSearchFilter()
        updateSearchDialog()
    }

    override fun onResume() {
        super.onResume()
        if (mGalleryViewModel.searchDialogShowing) {
            initializeSearchDialog()
            mSearchDialog?.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.gallery_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_option -> {
                if (mSearchDialog == null) initializeSearchDialog()
                mSearchDialog?.show()
                mGalleryViewModel.searchDialogShowing = true
                true
            }
            R.id.settings_option -> {
                val action = GalleryFragmentDirections.actionGalleryFragmentToSettingsFragment()
                findNavController().navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeSearchDialog() {
        mSearchDialog = Dialog(requireContext())
        mSearchDialog?.setContentView(R.layout.dialog_search)
        mSearchDialog?.setOnCancelListener { mGalleryViewModel.searchDialogShowing = false }

        val searchEditText = mSearchDialog?.findViewById<EditText>(R.id.search_edit_text)

        val dismissFab = mSearchDialog?.findViewById<FloatingActionButton>(R.id.search_dialog_exit_fab)
        dismissFab?.setOnClickListener {
            mGalleryViewModel.searchDialogShowing = false
            mSearchDialog?.dismiss()
        }

        val dueTodayCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_due_today_checkbox)
        val dueTomorrowCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_due_tomorrow_checkbox)
        val pendingCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_pending_checkbox)
        val scheduledCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_scheduled_checkbox)
        val unscheduledCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_unscheduled_checkbox)

        searchEditText?.setText(mGalleryViewModel.searchName)   // recover current search term

        searchEditText?.addTextChangedListener {
            val searchName = it.toString().trim()
            mGalleryViewModel.searchName = searchName
            updateSearchFilter()
        }

        // Handle search selection of scheduled / unscheduled projects
        dueTodayCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) mGalleryViewModel.searchType = SEARCH_TYPE_DUE_TODAY
            else mGalleryViewModel.searchType = SEARCH_TYPE_NONE
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        dueTomorrowCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) mGalleryViewModel.searchType = SEARCH_TYPE_DUE_TOMORROW
            else mGalleryViewModel.searchType = SEARCH_TYPE_NONE
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        pendingCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) mGalleryViewModel.searchType = SEARCH_TYPE_PENDING
            else mGalleryViewModel.searchType = SEARCH_TYPE_NONE
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        scheduledCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) mGalleryViewModel.searchType = SEARCH_TYPE_SCHEDULED
            else mGalleryViewModel.searchType = SEARCH_TYPE_NONE
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        unscheduledCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) mGalleryViewModel.searchType = SEARCH_TYPE_UNSCHEDULED
            else mGalleryViewModel.searchType = SEARCH_TYPE_NONE
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        updateSearchDialog()
    }

    private fun updateSearchFilter() {
        searchJob?.cancel()
        searchJob = mGalleryViewModel.viewModelScope.launch {
            mGalleryViewModel.displayedProjects = mGalleryViewModel.filterProjects()
            mGalleryAdapter?.setProjectData(mGalleryViewModel.displayedProjects)
            // show search fab if actively searching
            if (userIsNotSearching())
                mSearchActiveFAB?.hide()
            else
                mSearchActiveFAB?.show()
        }
    }

    private fun userIsNotSearching(): Boolean {
        return mGalleryViewModel.searchTags.isEmpty()
                && mGalleryViewModel.searchType == SEARCH_TYPE_NONE
                && mGalleryViewModel.searchName.isBlank()
    }

    private fun setupViewModel() {
        // Observe projects
        mGalleryViewModel.projects.observe(viewLifecycleOwner, Observer {
            // When the size of the list of projects increases assume we have added a project
            if (mPrevProjectsSize != null && mPrevProjectsSize!! < it.size) {
                // Then scroll to the end
                mGalleryRecyclerView?.scrollToPosition(mGalleryViewModel.displayedProjects.size)
            }
            mPrevProjectsSize = it.size
            mGalleryViewModel.viewModelScope.launch {
                mGalleryViewModel.displayedProjects = mGalleryViewModel.filterProjects()
                mGalleryAdapter?.setProjectData(mGalleryViewModel.displayedProjects)
            }
        })

        // Observe tags
        mGalleryViewModel.tags.observe(viewLifecycleOwner, Observer {
            updateSearchDialog()
        })
    }

    // Updates the dialog with all tags in the database for filtration
    private fun updateSearchDialog() {
        if (mSearchDialog == null) return

        // 1. Update the name edit text
        mSearchDialog?.findViewById<EditText>(R.id.search_edit_text)?.setText(mGalleryViewModel.searchName)

        // 2. Update tag state
        updateSearchDialogTags()

        // 3. Update the the checkboxes
        updateSearchDialogCheckboxes()
    }

    private fun updateSearchDialogTags() {
        var tags: List<TagEntry> = listOf()
        if (mGalleryViewModel.tags.value != null) {
            tags = mGalleryViewModel.tags.value!!.sortedBy { it.text.toLowerCase(Locale.getDefault()) }
        }
        // Clear the tag layout
        val tagLayout = mSearchDialog?.findViewById<FlexboxLayout>(R.id.dialog_search_tags_layout)
        val emptyListIndicator = mSearchDialog?.findViewById<TextView>(R.id.empty_tags_label)
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
            val tagCheckBox = CheckBox(requireContext())
            tagCheckBox.text = getString(R.string.hashtag, tag.text)
            tagCheckBox.setTypeface(null, Typeface.ITALIC)
            tagCheckBox.alpha = .8f
            tagCheckBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTag))
            tagCheckBox.isChecked = mGalleryViewModel.tagSelected(tag)
            tagCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) mGalleryViewModel.searchTags.add(tag)
                else mGalleryViewModel.searchTags.remove(tag)
                updateSearchFilter()
            }
            tagLayout?.addView(tagCheckBox)
        }
    }

    private fun updateSearchDialogCheckboxes() {
        // Update the state of search by schedule layout
        val dueTodayCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_due_today_checkbox)
        val dueTomorrowCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_due_tomorrow_checkbox)
        val pendingCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_pending_checkbox)
        val scheduledCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_scheduled_checkbox)
        val unscheduledCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_unscheduled_checkbox)
        dueTodayCheckBox?.isChecked = mGalleryViewModel.searchType == SEARCH_TYPE_DUE_TODAY
        dueTomorrowCheckBox?.isChecked = mGalleryViewModel.searchType == SEARCH_TYPE_DUE_TOMORROW
        pendingCheckBox?.isChecked = mGalleryViewModel.searchType == SEARCH_TYPE_PENDING
        scheduledCheckBox?.isChecked = mGalleryViewModel.searchType == SEARCH_TYPE_SCHEDULED
        unscheduledCheckBox?.isChecked = mGalleryViewModel.searchType == SEARCH_TYPE_UNSCHEDULED
    }

    override fun onClick(clickedProject: Project, binding: GalleryRecyclerviewItemBinding, position: Int) {
        // Prevents multiple clicks which cause a crash
        if (mLastClickTime != null && SystemClock.elapsedRealtime() - mLastClickTime!! < 250) return
        mLastClickTime = SystemClock.elapsedRealtime()

        // Navigate to the detail fragment
        val action = GalleryFragmentDirections.actionGalleryFragmentToDetailsFragment(clickedProject, position)
        val extras = FragmentNavigatorExtras(
                mAddProjectFAB as View to getString(R.string.key_add_transition),
                binding.projectImage to binding.projectImage.transitionName,
                binding.projectCardView to binding.projectCardView.transitionName,
                binding.galleryBottomGradient to binding.galleryBottomGradient.transitionName,
                binding.galleryScheduleLayout.galleryGradientTopDown to binding.galleryScheduleLayout.galleryGradientTopDown.transitionName,
                binding.galleryScheduleLayout.scheduleDaysUntilDueTv to binding.galleryScheduleLayout.scheduleDaysUntilDueTv.transitionName,
                binding.galleryScheduleLayout.scheduleIndicatorIntervalTv to binding.galleryScheduleLayout.scheduleIndicatorIntervalTv.transitionName
        )
        findNavController().navigate(action, extras)
    }

    // Restore recycler view state
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val recyclerState: Parcelable? = savedInstanceState?.getParcelable(BUNDLE_RECYCLER_LAYOUT)
        mGalleryRecyclerView?.layoutManager?.onRestoreInstanceState(recyclerState)
    }

    // Save recycler view state
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, mGalleryRecyclerView?.layoutManager?.onSaveInstanceState())
    }

    companion object {
        private var mNumberOfColumns = 3
        private val TAG = GalleryFragment::class.java.simpleName
        private const val BUNDLE_RECYCLER_LAYOUT = "recycler_layout_key"
    }
}
