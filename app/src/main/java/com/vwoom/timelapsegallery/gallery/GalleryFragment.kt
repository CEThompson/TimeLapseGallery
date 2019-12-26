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
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.RecyclerView
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

    private lateinit var recycler: RecyclerView

    private var mNumberOfColumns = 3
    private val TAG = GalleryFragment::class.java.simpleName
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mNewProjectFab = view.add_project_FAB

        // Increase columns for horizontal orientation
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) mNumberOfColumns = 6

        // Set up the adapter for the recycler view
        mGalleryAdapter = GalleryAdapter(this, this.requireContext())
        Log.d(TAG, "mProjectsAdapter is null: ${mGalleryAdapter == null}")
        // Set up the recycler view
        val gridLayoutManager = StaggeredGridLayoutManager(mNumberOfColumns, StaggeredGridLayoutManager.VERTICAL)

        val galleryRecyclerView = view.gallery_recycler_view
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
        mNewProjectFab?.setOnClickListener { v: View? ->
            val action = GalleryFragmentDirections.actionGalleryFragmentToCameraFragment(null)

            // TODO fix extras of shared elements
            val extras = FragmentNavigatorExtras(
                    mNewProjectFab as View to getString(R.string.key_add_transition)
            )
            findNavController().navigate(action, extras)
        }
        setupViewModel()
    }

    override fun onStop() {
        super.onStop()
        mNewProjectFab?.setOnClickListener(null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        val rootView = inflater.inflate(R.layout.fragment_gallery, container, false)
        val toolbar = rootView.gallery_fragment_toolbar
        (activity as TimeLapseGalleryActivity).setSupportActionBar(toolbar)
        toolbar.title = getString(R.string.app_name)
        (activity as TimeLapseGalleryActivity).supportActionBar?.setIcon(R.drawable.actionbar_space_between_icon_and_title)
        // TODO: Hunt down toolbar leaks
        // TODO: determine if setting up action bar with nav contoller is worth it
        //  (activity as TimeLapseGalleryActivity).setupActionBarWithNavController(findNavController())
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
        val action = GalleryFragmentDirections.actionGalleryFragmentToDetailsFragment(clickedProject, position)
        val extras = FragmentNavigatorExtras(
                mNewProjectFab as View to getString(R.string.key_add_transition),
                binding.projectImage to binding.projectImage.transitionName,
                binding.projectCardView to binding.projectCardView.transitionName
        )
        findNavController().navigate(action, extras)
    }

}
