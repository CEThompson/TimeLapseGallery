package com.vwoom.timelapsegallery.gallery

import android.content.res.Configuration
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.activities.GalleryFragmentDirections
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.utils.Keys
import kotlinx.android.synthetic.main.fragment_gallery.view.*


class GalleryFragment : Fragment(), GalleryAdapter.ProjectsAdapterOnClickHandler {

    var mNewProjectFab: FloatingActionButton? = null
    var mProjectsRecyclerView: RecyclerView? = null

    private var mGalleryAdapter: GalleryAdapter? = null
    private var mProjects: List<Project>? = null

    private var mNumberOfColumns = 3
    private val TAG = GalleryFragment::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

            }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val projectsRecyclerView = view.projects_recycler_view
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

        projectsRecyclerView.layoutManager = gridLayoutManager
        projectsRecyclerView.setHasFixedSize(false) // adjusting views at runtime
        projectsRecyclerView.adapter = mGalleryAdapter

        // Set up navigation to add new projects
        mNewProjectFab?.setOnClickListener { v: View? ->
            val action = GalleryFragmentDirections.actionGalleryFragmentToCameraFragment()

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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }


    // TODO figure out  why view model isn't setting up
    private fun setupViewModel() {
        Log.d(TAG, "setup view model in fragment fired")
        val viewModel = ViewModelProviders.of(this).get(GalleryViewModel::class.java)
        /* Observe projects */viewModel.projects.observe(this, Observer { projects: List<Project> ->
            mProjects = projects
            Log.d(TAG, "$mProjects")
            Log.d(TAG, "mProjects is null ${mProjects == null}")
            mGalleryAdapter?.setProjectData(projects)
            Log.d(TAG, "mNewProject fab is null: ${mNewProjectFab == null}")
            mNewProjectFab?.show()
        })
    }

    override fun onClick(clickedProject: Project, sharedElement: View, transitionName: String, position: Int) {
        val action = GalleryFragmentDirections.actionGalleryFragmentToDetailsFragment(clickedProject, position)
        val extras = FragmentNavigatorExtras(
                mNewProjectFab as View to Keys.ADD_FAB_TRANSITION_NAME,
                sharedElement to transitionName
        )
        findNavController().navigate(action, extras)
    }

}
