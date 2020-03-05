package com.vwoom.timelapsegallery.gallery

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.view.*
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.databinding.FragmentGalleryBinding
import com.vwoom.timelapsegallery.utils.InjectorUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

// TODO fix gallery leak
// TODO gallery slows down with usage, figure out why
class GalleryFragment : Fragment(), GalleryAdapter.GalleryAdapterOnClickHandler {
    
    private var mSearchDialog: Dialog? = null

    private lateinit var mGalleryAdapter: GalleryAdapter
    private lateinit var mGridLayoutManager: StaggeredGridLayoutManager

    private var tagJob: Job? = null

    private lateinit var mGalleryRecyclerView: RecyclerView
    private lateinit var mAddProjectFAB: FloatingActionButton
    private lateinit var mSearchActiveFAB: FloatingActionButton

    private val mGalleryViewModel: GalleryViewModel by viewModels {
        InjectorUtils.provideGalleryViewModelFactory(requireActivity())
    }

    // prevent doubleclicking
    private var mLastClickTime: Long? = null

    override fun onPause() {
        super.onPause()
        mSearchDialog?.dismiss()
    }

    override fun onStop() {
        super.onStop()
        tagJob?.cancel()
    }

    override fun onDestroyView() {
        // Prevent gallery recycler view from leaking by nullifying the adapter on detach
        // This is necessary because of transition animations
        mGalleryRecyclerView.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View?) {
            }
            override fun onViewDetachedFromWindow(v: View?) {
                mGalleryRecyclerView.adapter = null
            }
        })
        super.onDestroyView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = FragmentGalleryBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        // Set up options menu
        setHasOptionsMenu(true)
        val toolbar = binding.galleryFragmentToolbar
        (activity as TimeLapseGalleryActivity).setSupportActionBar(toolbar)
        toolbar.title = getString(R.string.app_name)
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
        mGalleryRecyclerView = binding.galleryRecyclerView
        mGalleryRecyclerView.apply {
            layoutManager = mGridLayoutManager
            setHasFixedSize(false)
            adapter = mGalleryAdapter
            postponeEnterTransition()
        }

        // TODO better handle transitioning during search filtration, this solution seems hacky
        if (mGalleryViewModel.currentProjects.isNotEmpty()) mGalleryAdapter.setProjectData(mGalleryViewModel.currentProjects)

        // TODO transition element breaks when filtering
        mGalleryRecyclerView.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                startPostponedEnterTransition()
                mGalleryRecyclerView.viewTreeObserver?.removeOnGlobalLayoutListener(this)
            }
        })

        // Set up navigation to add new projects
        mAddProjectFAB = binding.addProjectFAB
        mAddProjectFAB.setOnClickListener {
            (activity as TimeLapseGalleryActivity).setSupportActionBar(null)
            val action = GalleryFragmentDirections.actionGalleryFragmentToCameraFragment(null, null)
            findNavController().navigate(action)
        }

        mSearchActiveFAB = binding.searchActiveIndicator
        mSearchActiveFAB.setOnClickListener {
            // Clear search
            mGalleryViewModel.searchName = ""
            mGalleryViewModel.searchTags.clear()
            mGalleryViewModel.scheduleSearch = false
            mGalleryViewModel.unscheduledSearch = false
            updateSearchFilter()
            
            // Reset search dialog
            mSearchDialog?.findViewById<EditText>(R.id.search_edit_text)?.setText("")
            val tagsLayout = mSearchDialog?.findViewById<FlexboxLayout>(R.id.dialog_search_tags_layout)
            if (tagsLayout!=null) {
                val children = tagsLayout.children
                for (child in children){
                    val checkbox = child as CheckBox
                    checkbox.isChecked = false
                }
            }
            mSearchDialog?.findViewById<CheckBox>(R.id.search_scheduled_checkbox)?.isChecked = false
            mSearchDialog?.findViewById<CheckBox>(R.id.search_unscheduled_checkbox)?.isChecked = false
        }
        
        setupViewModel()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (mGalleryViewModel.searchDialogShowing) mSearchDialog?.show()
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
                // TODO: Determine if there is a better way to handle leaking toolbar references
                (activity as TimeLapseGalleryActivity).setSupportActionBar(null)
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

        val scheduledCheckbox = mSearchDialog?.findViewById<CheckBox>(R.id.search_scheduled_checkbox)
        val unscheduledCheckbox = mSearchDialog?.findViewById<CheckBox>(R.id.search_unscheduled_checkbox)
        val searchEditText = mSearchDialog?.findViewById<EditText>(R.id.search_edit_text)
        searchEditText?.setText(mGalleryViewModel.searchName)   // recover current search term

        searchEditText?.addTextChangedListener {
            val searchName = it.toString().trim()
            mGalleryViewModel.searchName = searchName
            // TODO re-evaluate use of search filter
            updateSearchFilter()
        }

        // Handle search selection of scheduled / unscheduled projects
        scheduledCheckbox?.setOnCheckedChangeListener { _, isChecked ->
            unscheduledCheckbox?.isEnabled = !isChecked
            mGalleryViewModel.scheduleSearch = isChecked
            // TODO re-evaluate use of search filter
            updateSearchFilter()
        }

        unscheduledCheckbox?.setOnCheckedChangeListener { _, isChecked ->
            scheduledCheckbox?.isEnabled = !isChecked
            mGalleryViewModel.unscheduledSearch = isChecked
            // TODO re-evaluate use of search filter
            updateSearchFilter()
        }

        if (mGalleryViewModel.tags.value!=null){
            val tags = mGalleryViewModel.tags.value!!
            val sortedTags: List<TagEntry> = tags.sortedBy {it.tag.toLowerCase(Locale.getDefault())}
            setTags(sortedTags)
        }
    }

    private fun updateSearchFilter() {
        tagJob?.cancel()
        tagJob = mGalleryViewModel.viewModelScope.launch {
            mGalleryViewModel.currentProjects = mGalleryViewModel.filterProjects()
            mGalleryAdapter.setProjectData(mGalleryViewModel.currentProjects)

            // show search fab if actively searching
            if (userIsNotSearching())
                mSearchActiveFAB.visibility = View.INVISIBLE
            else
                mSearchActiveFAB.visibility = View.VISIBLE
        }
    }

    private fun userIsNotSearching(): Boolean{
        return mGalleryViewModel.searchTags.isEmpty()
                && !mGalleryViewModel.scheduleSearch
                && !mGalleryViewModel.unscheduledSearch
                && mGalleryViewModel.searchName.isBlank()
    }

    private fun setupViewModel() {
        // Observe projects
        mGalleryViewModel.projects.observe(viewLifecycleOwner, Observer { projects: List<Project> ->
            mGalleryViewModel.viewModelScope.launch {
                mGalleryViewModel.allProjects = projects
                mGalleryViewModel.currentProjects = mGalleryViewModel.filterProjects()
                mGalleryAdapter.setProjectData(mGalleryViewModel.currentProjects)
                mGalleryRecyclerView.scrollToPosition(mGalleryViewModel.returnPosition)
            }
        })

        mGalleryViewModel.tags.observe(viewLifecycleOwner, Observer { tags: List<TagEntry> ->
            val sortedTags = tags.sortedBy {it.tag.toLowerCase(Locale.getDefault())}
            setTags(sortedTags)
        })
    }

    // Updates the dialog with all tags in the database for filtration
    private fun setTags(tags: List<TagEntry>) {
        // Clear the tag layout
        val tagLayout = mSearchDialog?.findViewById<FlexboxLayout>(R.id.dialog_search_tags_layout)
        val emptyListIndicator = mSearchDialog?.findViewById<TextView>(R.id.empty_tags_label)
        tagLayout?.removeAllViews()

        if (tags.isEmpty()) {
            emptyListIndicator?.visibility = View.VISIBLE
            tagLayout?.visibility = View.GONE
            return
        } else {
            tagLayout?.visibility = View.VISIBLE
            emptyListIndicator?.visibility = View.GONE
        }

        // Create the tag views
        for (tag in tags) {
            val tagCheckBox = CheckBox(requireContext())
            tagCheckBox.text = getString(R.string.hashtag, tag.tag)
            tagCheckBox.isChecked = mGalleryViewModel.tagSelected(tag)
            tagCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) mGalleryViewModel.searchTags.add(tag)
                else mGalleryViewModel.searchTags.remove(tag)
                updateSearchFilter()
            }
            tagLayout?.addView(tagCheckBox)
        }
    }

    override fun onClick(clickedProject: Project, projectImageView: ImageView, projectCardView: CardView, position: Int) {
        // Prevents multiple clicks which cause a crash
        if (mLastClickTime != null && SystemClock.elapsedRealtime() - mLastClickTime!! < 1000) return
        mLastClickTime = SystemClock.elapsedRealtime()

        // Save the position of the first visible item in the gallery
        val firstItems = IntArray(mNumberOfColumns)
        mGridLayoutManager.findFirstCompletelyVisibleItemPositions(firstItems)
        mGalleryViewModel.returnPosition = firstItems[0]

        // Navigate to the detail fragment
        val action = GalleryFragmentDirections.actionGalleryFragmentToDetailsFragment(clickedProject, position)
        val extras = FragmentNavigatorExtras(
                mAddProjectFAB as View to getString(R.string.key_add_transition),
                projectImageView to projectImageView.transitionName,
                projectCardView to projectCardView.transitionName
        )
        findNavController().navigate(action, extras)
    }

    companion object {
        private var mNumberOfColumns = 3
        private val TAG = GalleryFragment::class.java.simpleName
    }
}
