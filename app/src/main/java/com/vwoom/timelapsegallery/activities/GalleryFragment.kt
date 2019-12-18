package com.vwoom.timelapsegallery.activities

import android.content.res.Configuration
import android.os.Bundle
import android.transition.TransitionInflater
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
import butterknife.BindView

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.adapters.ProjectsAdapter
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.utils.Keys
import com.vwoom.timelapsegallery.viewmodels.MainActivityViewModel


class GalleryFragment : Fragment(), ProjectsAdapter.ProjectsAdapterOnClickHandler {

    @BindView(R.id.add_project_FAB)
    var mNewProjectFab: FloatingActionButton? = null
    @BindView(R.id.projects_recycler_view)
    var mProjectsRecyclerView: RecyclerView? = null
    private var mProjectsAdapter: ProjectsAdapter? = null
    private var mProjects: List<Project>? = null
    private var mNumberOfColumns = 3
    private val TAG = GalleryFragment::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO implement shared element transition
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_shared_element_transition)

        // Increase columns for horizontal orientation
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) mNumberOfColumns = 6

        // Set up the adapter for the recycler view
        mProjectsAdapter = ProjectsAdapter(this, this.context)

        // Set up the recycler view
        val gridLayoutManager = StaggeredGridLayoutManager(mNumberOfColumns, StaggeredGridLayoutManager.VERTICAL)
        mProjectsRecyclerView!!.layoutManager = gridLayoutManager
        mProjectsRecyclerView!!.setHasFixedSize(false) // adjusting views at runtime
        mProjectsRecyclerView!!.adapter = mProjectsAdapter

        // Set up navigation to add new projects
        mNewProjectFab!!.setOnClickListener { v: View? ->
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


    private fun setupViewModel() {
        val viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        /* Observe projects */viewModel.projects.observe(this, Observer { projects: List<Project> ->
            mProjects = projects
            mProjectsAdapter!!.setProjectData(projects)
            mNewProjectFab!!.show()
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
