package com.vwoom.timelapsegallery.activities

import android.app.ActivityOptions
import android.app.SharedElementCallback
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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import butterknife.BindView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.adapters.ProjectsAdapter
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.utils.Keys
import com.vwoom.timelapsegallery.viewmodels.MainActivityViewModel


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [GalleryFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [GalleryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class GalleryFragment : Fragment(), ProjectsAdapter.ProjectsAdapterOnClickHandler {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    /* Variables from activity*/
    @BindView(R.id.add_project_FAB)
    var mNewProjectFab: FloatingActionButton? = null
    @BindView(R.id.projects_recycler_view)
    var mProjectsRecyclerView: RecyclerView? = null
    private var mProjectsAdapter: ProjectsAdapter? = null
    private var mProjects: List<Project>? = null
    private var mNumberOfColumns = 3
    private val TAG = GalleryFragment::class.java.simpleName

    /* Shared Element Position information */
    private var mReenterState: Bundle? = null
    private val mCallback: SharedElementCallback = object : SharedElementCallback() {
        override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) { // If reenter state contains bundle the activity is returning
            if (mReenterState != null) {
                Log.d(TAG, "shared elements: main activity callback firing")
                val transitionName = mReenterState!!.getString(Keys.TRANSITION_NAME)
                val photoView: View? = mProjectsRecyclerView?.findViewWithTag(transitionName)
                Log.d(TAG, "shared elements: transition name is $transitionName")
                if (photoView != null && transitionName != null) {
                    names.clear()
                    names.add(transitionName)
                    sharedElements.clear()
                    sharedElements[transitionName] = photoView
                }
                mReenterState = null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }



        // Increase columns for horizontal orientation
        // Increase columns for horizontal orientation
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) mNumberOfColumns = 6

        // Set up the adapter for the recycler view
        // Set up the adapter for the recycler view
        mProjectsAdapter = ProjectsAdapter(this, this)

        // Set up the recycler view
        // Set up the recycler view
        val gridLayoutManager = StaggeredGridLayoutManager(mNumberOfColumns, StaggeredGridLayoutManager.VERTICAL)
        mProjectsRecyclerView!!.layoutManager = gridLayoutManager
        mProjectsRecyclerView!!.setHasFixedSize(false) // adjusting views at runtime

        mProjectsRecyclerView!!.adapter = mProjectsAdapter

        // Set up click listener to add new projects
        // Set up click listener to add new projects
        mNewProjectFab!!.setOnClickListener { v: View? ->
            val newProjectIntent = Intent(this@MainActivity, CameraActivity::class.java)
            startActivity(newProjectIntent)
        }

        prepareSharedElementTransition()
        mFilter = getIntent().getBooleanExtra(Keys.PROJECT_FILTER_BY_SCHEDULED_TODAY, false)

        // Set up the view model
        // Set up the view model
        setupViewModel()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment GalleryFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                GalleryFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
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

    fun onActivityReenter(resultCode: Int, data: Intent) {
        super.onActivityReenter(resultCode, data)
        Log.d(FragmentActivity.TAG, "shared elements: reentering activity")
        mReenterState = Bundle(data.extras)
        val returnPosition = mReenterState!!.getInt(Keys.TRANSITION_POSITION)
        mProjectsRecyclerView!!.scrollToPosition(returnPosition)
        Log.d(FragmentActivity.TAG, "shared elements: scrolling to position $returnPosition")
        postponeEnterTransition()
        schedulePostponedTransition()
    }

    private fun schedulePostponedTransition() {
        mProjectsRecyclerView!!.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                mProjectsRecyclerView!!.viewTreeObserver.removeOnPreDrawListener(this)
                mProjectsRecyclerView!!.requestLayout()
                startPostponedEnterTransition()
                return true
            }
        })
    }

    private fun prepareSharedElementTransition() {
        val transition = TransitionInflater.from(this)
                .inflateTransition(R.transition.image_shared_element_transition)
        getWindow().setSharedElementExitTransition(transition)
        setExitSharedElementCallback(mCallback)
    }
}
