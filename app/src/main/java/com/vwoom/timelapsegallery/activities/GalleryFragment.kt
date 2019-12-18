package com.vwoom.timelapsegallery.activities

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
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
        // Increase columns for horizontal orientation
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) mNumberOfColumns = 6

        // Set up the adapter for the recycler view
        mProjectsAdapter = ProjectsAdapter(this, this.context)

        // Set up the recycler view
        val gridLayoutManager = StaggeredGridLayoutManager(mNumberOfColumns, StaggeredGridLayoutManager.VERTICAL)
        mProjectsRecyclerView!!.layoutManager = gridLayoutManager
        mProjectsRecyclerView!!.setHasFixedSize(false) // adjusting views at runtime

        mProjectsRecyclerView!!.adapter = mProjectsAdapter

        // Set up click listener to add new projects
        mNewProjectFab!!.setOnClickListener { v: View? ->
            val action = GalleryFragmentDirections.actionGalleryFragmentToCameraFragment()
            findNavController().navigate(action)
        }

        // Set up the view model
        setupViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }


    /* Sets up view models */
    private fun setupViewModel() {
        val viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        /* Observe projects */viewModel.projects.observe(this, Observer { projects: List<Project?> ->
            mProjects = projects
            mProjectsAdapter!!.setProjectData(projects)
            mNewProjectFab!!.show()
        })
    }

    override fun onClick(clickedProject: Project?, sharedElement: View, transitionName: String, position: Int) {
        val intent = Intent(this, DetailsActivity::class.java)
        intent.putExtra(Keys.PROJECT_ENTRY, clickedProject)
        intent.putExtra(Keys.TRANSITION_POSITION, position)
        val p1 = Pair.create(sharedElement, transitionName)
        val p2 = Pair.create<View, String>(mNewProjectFab, Keys.ADD_FAB_TRANSITION_NAME)
        // Start the activity with a shared element if lollipop or higher
        val bundle = ActivityOptions
                .makeSceneTransitionAnimation(this@MainActivity,
                        p1,
                        p2)
                .toBundle()
        startActivity(intent, bundle)
    }
}
