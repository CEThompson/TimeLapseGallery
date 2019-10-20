package com.vwoom.timelapsegallery.activities

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.notification.NotificationUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils

const val TAG = "Settings Tracker"

class SettingsActivity : AppCompatActivity(), TaskFragment.TaskCallbacks, SettingsFragment.TaskCallbacks {

    // Fragments
    var mSettingsFragment: Fragment? = null
    var mTaskFragment: Fragment? = null

    // Dialogs
    var mSyncDialog: Dialog? = null
    var mFileModDialog: Dialog? = null
    var mVerifySyncDialog: Dialog? = null

    // State
    var mSyncing: Boolean? = null
    var mShowingFileModDialog: Boolean? = null
    var mShowingVerifySyncDialog: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        mSettingsFragment = supportFragmentManager.findFragmentByTag("settings_fragment")
        mTaskFragment = supportFragmentManager.findFragmentByTag("task_fragment")

        if (mSettingsFragment == null) mSettingsFragment = SettingsFragment()
        if (mTaskFragment == null) mTaskFragment = TaskFragment()

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, mSettingsFragment as PreferenceFragmentCompat, "settings_fragment")
                .commit()

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.task_container, mTaskFragment as TaskFragment, "task_fragment")
                .commit()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set up dialogs
        createSyncDialog()
        createVerifyProjectImportDialog()
        createManualFileModificationDialog()

        // Restore instance state of sync dialog
        if (savedInstanceState != null) {
            mSyncing = savedInstanceState.getBoolean("mSyncing", false)
            mShowingFileModDialog = savedInstanceState.getBoolean("mShowingFileModDialog", false)
            mShowingVerifySyncDialog = savedInstanceState.getBoolean("mShowingVerifySyncDialog", false)

            if (mSyncing!!) showSyncDialog()
            else if (mShowingFileModDialog!!) showFileModificationDialog()
            else if (mShowingVerifySyncDialog!!) showVerifyProjectImportDialog()

            Log.d(TAG, "restored instance state, mSyncing is $mSyncing, mShowingFileModDialog is" +
                    " $mShowingFileModDialog, mShowingVerifySync is $mShowingVerifySyncDialog")
        }
    }

    fun createSyncDialog(){
        Log.d(TAG, "creating sync dialog")
        mSyncDialog = Dialog(this)
        mSyncDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        // Set the dialog
        mSyncDialog?.setCancelable(false)
        mSyncDialog?.setContentView(R.layout.sync_dialog)

        // Set up the button
        val button = mSyncDialog?.findViewById(R.id.sync_verification_button) as androidx.appcompat.widget.AppCompatButton
        button.setOnClickListener {
            mSyncDialog?.dismiss()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "saving instance state")
        super.onSaveInstanceState(outState)
        if (mSyncing == true) outState.putBoolean("mSyncing", true)
        if (mShowingFileModDialog == true) outState.putBoolean("mShowingFileModDialog", true)
        if (mShowingVerifySyncDialog == true) outState.putBoolean("mShowingVerifySyncDialog", true)
    }

    /*
    override fun onPause() {
        super.onPause()
        if (mSyncDialog != null){
            if (mSyncDialog?.isShowing!!)
                mSyncDialog?.dismiss()
        }
    }
    */

    // Shows a dialog to give progress feedback on synchronization
    fun showSyncDialog(){
        Log.d(TAG, "showing sync dialog")
        // Set the response
        val responseView = mSyncDialog?.findViewById(R.id.sync_response) as TextView
        responseView.setText(this.getString(R.string.executing_sync_notification))
        val progress = mSyncDialog?.findViewById(R.id.sync_progress) as ProgressBar
        /// Set the image feedback
        val imageFeedback = mSyncDialog?.findViewById(R.id.sync_feedback_image) as ImageView
        val button = mSyncDialog?.findViewById(R.id.sync_verification_button) as androidx.appcompat.widget.AppCompatButton

        // Show the updated views
        progress.visibility = View.VISIBLE
        imageFeedback.visibility = View.INVISIBLE
        button.visibility = View.GONE
        // Show the dialog
        mSyncDialog?.show()
    }

    // Updates the dialog showing progress on synchronization
    fun updateSyncDialog(response: String){
        Log.d(TAG, "updating sync dialog")
        val success = (response.equals(getString(R.string.valid_file_structure)))

        // Set the response
        val responseView = mSyncDialog?.findViewById(R.id.sync_response) as TextView
        responseView.setText(response)

        val progress = mSyncDialog?.findViewById(R.id.sync_progress) as ProgressBar

        /// Set the image feedback
        val imageFeedback = mSyncDialog?.findViewById(R.id.sync_feedback_image) as ImageView
        if (success) {
            imageFeedback.setImageResource(R.drawable.ic_check_green_40dp)
            responseView.setText(getString(R.string.executing_sync_complete))
        }
        else imageFeedback.setImageResource(R.drawable.ic_error_red_40dp)

        val button = mSyncDialog?.findViewById(R.id.sync_verification_button) as androidx.appcompat.widget.AppCompatButton

        // Show the updated views
        progress.visibility = View.INVISIBLE
        imageFeedback.visibility = View.VISIBLE
        button.visibility = View.VISIBLE
    }

    /* Async Task Callbacks */
    override fun onPostExecute(response: String) {
        Log.d(TAG, "onPostExecute: setting mSyncing to false and updating dialog")
        mSyncing = false
        updateSyncDialog(response)
    }

    override fun onPreExecute() {
        Log.d(TAG, "onPreExecute: setting mSyncing to true and show dialog")
        mSyncing = true
        showSyncDialog()
    }

    override fun onCancelled() {
        // Do nothing
    }

    override fun onProgressUpdate(percent: Int) {
        // Do nothing
    }

    /* Settings Fragment Callbacks */
    override fun showVerifyProjectImportDialog() {
        mShowingVerifySyncDialog = true
        mVerifySyncDialog?.show()
    }

    fun createVerifyProjectImportDialog(){
        val context = this
        val builder = AlertDialog.Builder(context)
                .setTitle(R.string.warning)
                .setMessage(R.string.database_sync_warning)
                .setPositiveButton(R.string.ok) { _, _ ->
                    Log.d("settings activity", "Launching database sync asynct task")
                    // Execute the sync in the background
                    (mTaskFragment as TaskFragment).executeDatabaseSync()
                    mShowingVerifySyncDialog = false
                }
                .setNegativeButton(R.string.cancel){_,_ -> mShowingVerifySyncDialog = false}
                .setIcon(R.drawable.ic_warning_black_24dp)

        mVerifySyncDialog = builder.create()
    }

    override fun showFileModificationDialog() {
        mShowingFileModDialog = true
        mFileModDialog?.show()
    }

    fun createManualFileModificationDialog() {
        val builder = AlertDialog.Builder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.file_modification_information)
                .setPositiveButton(R.string.ok) { _, _ -> mShowingFileModDialog = false}
                .setIcon(R.drawable.ic_warning_black_24dp)
        mFileModDialog = builder.create()
    }
}

class TaskFragment: Fragment() {
    var mCallbacks: TaskCallbacks? = null

    interface TaskCallbacks {
        fun onPreExecute()
        fun onProgressUpdate(percent: Int)
        fun onCancelled()
        fun onPostExecute(response: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mCallbacks = context as TaskCallbacks
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onDetach() {
        super.onDetach()
        mCallbacks = null
    }

    fun executeDatabaseSync(){
        DatabaseSyncTask().execute(this.context!!)
    }
    // This async task validates the files are in correct format then synchronizes to the database
    inner class DatabaseSyncTask : AsyncTask<Context, Void, String>() {

        // Do in background validates then executes import
        override fun doInBackground(vararg p0: Context): String {
            val context = p0.get(0)
            val response = ProjectUtils.validateFileStructure(context)

            // If modified files are valid go ahead notify user of background work and import
            if (response.equals(context.getString(R.string.valid_file_structure))){
                ProjectUtils.importProjects(context)
            }

            return response
        }

        override fun onPreExecute() {
            super.onPreExecute()
            mCallbacks?.onPreExecute()
        }

        // On post execute updates sync dialog
        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            mCallbacks?.onPostExecute(result)
        }
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    var mCallbacks: TaskCallbacks? = null
    var prefs: SharedPreferences? = null
    var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null


    interface TaskCallbacks {
        fun showFileModificationDialog()
        fun showVerifyProjectImportDialog()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mCallbacks = context as TaskCallbacks
    }

    override fun onDetach() {
        super.onDetach()
        mCallbacks = null
    }

    override fun onResume() {
        super.onResume()
        prefs?.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onPause() {
        super.onPause()
        prefs?.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val syncPref: Preference? =  findPreference(getString(R.string.key_sync))

        // Listen for changes to shared preferences and update notification worker on change
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            Log.d("settings activity", "Notification listener activitating for key = $key")

            // If the user changes the notifications enabled preference trigger the notification worker to update any alarms
            if (key.equals(this.getString(R.string.key_notifications_enabled))) {
                NotificationUtils.scheduleNotificationWorker(activity);
            }

            // Same for notification time
            if (key.equals(this.getString(R.string.key_notification_time))) {
                NotificationUtils.scheduleNotificationWorker(activity)
            }

            // If the user enables manual file mSyncing give some info
            if (key.equals(getString(R.string.key_sync_allowed))){
                val isSyncAllowed = prefs.getBoolean(key, false)
                if (isSyncAllowed) mCallbacks?.showFileModificationDialog()
            }
        }

        // TODO (update) write unit test for file / database sync
        // Verify the user wants to sync files to the database
        syncPref?.setOnPreferenceClickListener{
            mCallbacks?.showVerifyProjectImportDialog()
            true
        }
    }
}
