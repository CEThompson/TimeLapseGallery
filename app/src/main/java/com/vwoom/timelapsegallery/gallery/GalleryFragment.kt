package com.vwoom.timelapsegallery.gallery

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.databinding.FragmentGalleryBinding
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.utils.InjectorUtils

// TODO add search option?
class GalleryFragment : Fragment(), GalleryAdapter.GalleryAdapterOnClickHandler {

    private var mFilterDialog: Dialog? = null
    private var mGalleryAdapter: GalleryAdapter? = null
    private var mProjects: List<Project>? = null

    private lateinit var mBinding: FragmentGalleryBinding

    override fun onDestroyView() {
        super.onDestroyView()
        mGalleryAdapter = null
    }

    // TODO return transition works, but not adapter does not update appropriately: figure this out
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (::mBinding.isInitialized) return mBinding.root

        mBinding = FragmentGalleryBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

        // Set up options menu
        setHasOptionsMenu(true)
        val toolbar = mBinding.galleryFragmentToolbar
        (activity as TimeLapseGalleryActivity).setSupportActionBar(toolbar)
        toolbar.title = getString(R.string.app_name)
        (activity as TimeLapseGalleryActivity).supportActionBar?.setIcon(R.drawable.actionbar_space_between_icon_and_title)
        // TqODO: determine if setting up action bar with nav contoller is worth it
        //  (activity as TimeLapseGalleryActivity).setupActionBarWithNavController(findNavController())

        // Increase columns for horizontal orientation
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) mNumberOfColumns = 6
        else mNumberOfColumns = 3

        // Set up the adapter for the recycler view
        mGalleryAdapter = GalleryAdapter(this, this.requireContext())

        // Set up the recycler view
        val gridLayoutManager = StaggeredGridLayoutManager(mNumberOfColumns, StaggeredGridLayoutManager.VERTICAL)
        val galleryRecyclerView = mBinding.galleryRecyclerView
        galleryRecyclerView.apply {
            layoutManager = gridLayoutManager
            setHasFixedSize(false)
            adapter = mGalleryAdapter
            postponeEnterTransition()
            viewTreeObserver.addOnPreDrawListener(object: ViewTreeObserver.OnPreDrawListener{
                override fun onPreDraw(): Boolean {
                    startPostponedEnterTransition()
                    viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            })
        }

        // Set up navigation to add new projects
        mBinding.addProjectFAB.setOnClickListener {
            // TODO: Determine if there is a better way to handle leaking toolbar references
            // Note: navigating from gallery to detail results in activity leaking toolbar as reference
            (activity as TimeLapseGalleryActivity).setSupportActionBar(null)
            val action = GalleryFragmentDirections.actionGalleryFragmentToCameraFragment(null,null)
            findNavController().navigate(action)
        }

        // TODO convert dialog initializations to lazy?
        initializeFilterDialog()

        setupViewModel()

        return mBinding.root
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.gallery_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.settings_option -> {
                // TODO: Determine if there is a better way to handle leaking toolbar references
                (activity as TimeLapseGalleryActivity).setSupportActionBar(null)
                val action = GalleryFragmentDirections.actionGalleryFragmentToSettingsFragment()
                findNavController().navigate(action)
                true
            }
            R.id.filter_option -> {
                // TODO open dialoge to filter projects here
                mFilterDialog?.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeFilterDialog(){
        mFilterDialog = Dialog(requireContext())
        mFilterDialog?.setContentView(R.layout.dialog_filter)
    }

    private fun setupViewModel() {
        val viewModel = InjectorUtils.provideGalleryViewModel(requireContext())

        // Observe projects
        viewModel.projects.observe(this, Observer { projects: List<Project> ->
            mProjects = projects
            mGalleryAdapter?.setProjectData(projects)
        })

        viewModel.tags.observe(this, Observer { tags: List<TagEntry> ->
            // TODO update tags here
            setFilterDialogTags(tags)
        })
    }

    private fun setFilterDialogTags(tags: List<TagEntry>){
        val tagLayout = mFilterDialog?.findViewById<LinearLayout>(R.id.dialog_filter_tags_layout)
        tagLayout?.removeAllViews()
        for (tag in tags){
            val tagCheckBox: CheckBox = CheckBox(requireContext())
            tagCheckBox.text = tag.tag
            tagLayout?.addView(tagCheckBox)
        }
    }

    override fun onClick(clickedProject: Project, binding: GalleryRecyclerviewItemBinding, position: Int) {
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
