package com.vwoom.timelapsegallery.gallery

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.flexbox.FlexboxLayout
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.databinding.FragmentGalleryBinding
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.utils.InjectorUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GalleryFragment : Fragment(), GalleryAdapter.GalleryAdapterOnClickHandler {

    private var mFilterDialog: Dialog? = null
    private var mGalleryAdapter: GalleryAdapter? = null
    private var mGridLayoutManager: StaggeredGridLayoutManager? = null
    private var mProjects: List<Project>? = null

    private var mFilterTags: ArrayList<TagEntry> = arrayListOf()

    private lateinit var mBinding: FragmentGalleryBinding

    private var tagJob: Job? = null

    private val mGalleryViewModel: GalleryViewModel by viewModels {
        InjectorUtils.provideGalleryViewModelFactory(requireActivity())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mGalleryAdapter = null
    }

    override fun onPause() {
        super.onPause()
        mFilterDialog?.dismiss()
    }

    override fun onStop() {
        super.onStop()
        tagJob?.cancel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (::mBinding.isInitialized) postponeEnterTransition()
        mBinding = FragmentGalleryBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        // Set up options menu
        setHasOptionsMenu(true)
        val toolbar = mBinding.galleryFragmentToolbar
        (activity as TimeLapseGalleryActivity).setSupportActionBar(toolbar)
        toolbar.title = getString(R.string.app_name)
        (activity as TimeLapseGalleryActivity).supportActionBar?.setIcon(R.drawable.actionbar_space_between_icon_and_title)

        // Increase columns for horizontal orientation
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) mNumberOfColumns = 6
        else mNumberOfColumns = 3

        // Set up the adapter for the recycler view
        mGalleryAdapter = GalleryAdapter(this, this.requireContext())

        // Set up the recycler view
        mGridLayoutManager = StaggeredGridLayoutManager(mNumberOfColumns, StaggeredGridLayoutManager.VERTICAL)
        val galleryRecyclerView = mBinding.galleryRecyclerView
        galleryRecyclerView.apply {
            layoutManager = mGridLayoutManager
            setHasFixedSize(false)
            adapter = mGalleryAdapter
            //postponeEnterTransition()
        }

        // Set up navigation to add new projects
        mBinding.addProjectFAB.setOnClickListener {
            // TODO: Determine if there is a better way to handle leaking toolbar references
            // Note: navigating from gallery to detail results in activity leaking toolbar as reference
            (activity as TimeLapseGalleryActivity).setSupportActionBar(null)
            val action = GalleryFragmentDirections.actionGalleryFragmentToCameraFragment(null,null)
            findNavController().navigate(action)
        }

        initializeFilterDialog()

        setupViewModel()

        return mBinding.root
    }

    override fun onResume() {
        super.onResume()
        if (mGalleryViewModel.filterDialogShowing) mFilterDialog?.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.gallery_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.search_option -> {
                // TODO implement search
                mFilterDialog?.show()
                mGalleryViewModel.filterDialogShowing = true
                true
            }
            R.id.settings_option -> {
                // TODO: Determine if there is a better way to handle leaking toolbar references
                (activity as TimeLapseGalleryActivity).setSupportActionBar(null)
                val action = GalleryFragmentDirections.actionGalleryFragmentToSettingsFragment()
                findNavController().navigate(action)
                true
            }
            R.id.filter_option -> {
                // TODO implement filter
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeFilterDialog(){
        mFilterDialog = Dialog(requireContext())
        mFilterDialog?.setContentView(R.layout.dialog_search)
        mFilterDialog?.setOnCancelListener{mGalleryViewModel.filterDialogShowing=false}
    }

    private fun updateSearchFilter(){
        tagJob?.cancel()
        mGalleryViewModel.setFilter(mFilterTags)
        tagJob = mGalleryViewModel.viewModelScope.launch {
            val filteredProjects = mGalleryViewModel.filterProjects(mProjects!!)
            mGalleryAdapter?.setProjectData(filteredProjects)
        }
    }

    private fun setupViewModel() {
        // Observe projects
        mGalleryViewModel.projects.observe(this, Observer { projects: List<Project> ->
            mGalleryViewModel.viewModelScope.launch {
                mProjects = projects
                val filteredProjects = mGalleryViewModel.filterProjects(projects)
                mGalleryAdapter?.setProjectData(filteredProjects)
                mBinding.galleryRecyclerView.scrollToPosition(mGalleryViewModel.returnPosition)
                startPostponedEnterTransition()
            }
        })

        mGalleryViewModel.tags.observe(this, Observer { tags: List<TagEntry> ->
            setTags(tags.sortedBy {it.tag.toLowerCase()})
        })
    }

    // Updates the dialog with all tags in the database for filtration
    private fun setTags(tags: List<TagEntry>){
        // Clear the tag layout
        val tagLayout = mFilterDialog?.findViewById<FlexboxLayout>(R.id.dialog_search_tags_layout)
        val emptyListIndicator = mFilterDialog?.findViewById<TextView>(R.id.empty_tags_label)
        tagLayout?.removeAllViews()

        if (tags.isEmpty()){
            emptyListIndicator?.visibility = View.VISIBLE
            tagLayout?.visibility = View.GONE
            return
        } else {
            tagLayout?.visibility = View.VISIBLE
            emptyListIndicator?.visibility = View.GONE
        }

        // Create the tag views
        for (tag in tags){
            val tagCheckBox = CheckBox(requireContext())
            tagCheckBox.text = getString(R.string.hashtag, tag.tag)
            tagCheckBox.isChecked = mGalleryViewModel.tagSelected(tag)
            tagCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) mFilterTags.add(tag)
                else mFilterTags.remove(tag)
                updateSearchFilter()
            }
            tagLayout?.addView(tagCheckBox)
        }
    }

    override fun onClick(clickedProject: Project, binding: GalleryRecyclerviewItemBinding, position: Int) {
        // Save the position of the first visible item in the gallery
        val firstItems = IntArray(mNumberOfColumns)
        mGridLayoutManager?.findFirstCompletelyVisibleItemPositions(firstItems)
        mGalleryViewModel.returnPosition = firstItems[0]

        // Navigate to the detail fragment
        val action = GalleryFragmentDirections.actionGalleryFragmentToDetailsFragment(clickedProject, position)
        val extras = FragmentNavigatorExtras(
                mBinding.addProjectFAB as View to getString(R.string.key_add_transition),
                binding.projectImage to binding.projectImage.transitionName,
                binding.projectCardView to binding.projectCardView.transitionName
        )
        findNavController().navigate(action, extras)
    }

    companion object {
        private var mNumberOfColumns = 3
        private val TAG = GalleryFragment::class.java.simpleName
    }
}
