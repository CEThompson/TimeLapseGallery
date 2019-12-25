package com.vwoom.timelapsegallery.gallery

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.utils.InjectorUtils
import com.vwoom.timelapsegallery.utils.Keys
import kotlinx.android.synthetic.main.fragment_gallery.view.*


class GalleryFragment : Fragment(), GalleryAdapter.GalleryAdapterOnClickHandler {

    var mNewProjectFab: FloatingActionButton? = null

    private var mGalleryAdapter: GalleryAdapter? = null
    private var mProjects: List<Project>? = null

    private var mNumberOfColumns = 3
    private val TAG = GalleryFragment::class.java.simpleName

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val galleryRecyclerView = view.gallery_recycler_view
        mNewProjectFab = view.add_project_FAB

        // TODO implement shared element transition
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_shared_element_transition)

        // Increase columns for horizontal orientation
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) mNumberOfColumns = 6

        // Set up the adapter for the recycler view
        mGalleryAdapter = GalleryAdapter(this, this.requireContext())
        Log.d(TAG, "mProjectsAdapter is null: ${mGalleryAdapter == null}")
        // Set up the recycler view
        val gridLayoutManager = StaggeredGridLayoutManager(mNumberOfColumns, StaggeredGridLayoutManager.VERTICAL)

        galleryRecyclerView.layoutManager = gridLayoutManager
        galleryRecyclerView.setHasFixedSize(false) // adjusting views at runtime
        galleryRecyclerView.adapter = mGalleryAdapter

        // Set up navigation to add new projects
        mNewProjectFab?.setOnClickListener { v: View? ->
            val action = GalleryFragmentDirections.actionGalleryFragmentToCameraFragment(null)

            // TODO fix extras of shared elements
            val extras = FragmentNavigatorExtras(
                    mNewProjectFab as View to Keys.ADD_FAB_TRANSITION_NAME
            )
            findNavController().navigate(action, extras)
        }
        setupViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        val rootView = inflater.inflate(R.layout.fragment_gallery, container, false)
        val toolbar = rootView.gallery_fragment_toolbar
        (activity as TimeLapseGalleryActivity).setSupportActionBar(toolbar)
        toolbar.title = getString(R.string.app_name)
        (activity as TimeLapseGalleryActivity).supportActionBar?.setIcon(R.drawable.actionbar_space_between_icon_and_title)
        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.gallery_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.settings_option -> {
                val action = GalleryFragmentDirections.actionGalleryFragmentToSettingsFragment()
                findNavController().navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupViewModel() {
        val viewModel = InjectorUtils.provideGalleryViewModel(requireContext())
        /* Observe projects */viewModel.projects.observe(this, Observer { projects: List<Project> ->
            mProjects = projects
            mGalleryAdapter?.setProjectData(projects)
        })
    }

    override fun onClick(clickedProject: Project, binding: GalleryRecyclerviewItemBinding, position: Int) {
        val imageTransitionName = clickedProject.project_id.toString()
        val cardTransitionName = imageTransitionName + "card"

        // Set transition targets
        binding.projectImage.transitionName = imageTransitionName
        binding.projectCardView.transitionName = cardTransitionName

        val action = GalleryFragmentDirections.actionGalleryFragmentToDetailsFragment(clickedProject, position)
        val extras = FragmentNavigatorExtras(
                mNewProjectFab as View to Keys.ADD_FAB_TRANSITION_NAME,
                binding.projectImage to imageTransitionName,
                binding.projectCardView to cardTransitionName
        )
        findNavController().navigate(action, extras)
    }

}
