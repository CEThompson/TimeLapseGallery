package com.vwoom.timelapsegallery.activities

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.os.PersistableBundle
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

class SettingsActivity : AppCompatActivity(), SettingsFragment.TaskCallbacks {

    var settingsFragment: Fragment? = null
    var syncDialog: Dialog? = null

    var syncing: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        settingsFragment = supportFragmentManager.findFragmentByTag("settings_fragment")
        if (settingsFragment == null) settingsFragment = SettingsFragment()

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, settingsFragment as PreferenceFragmentCompat, "settings_fragment")
                .commit()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        createSyncDialog()

        // Restore instance state of sync dialog
        if (savedInstanceState != null) {
            syncing = savedInstanceState.getBoolean("syncing", false)
            if (syncing!!) showSyncDialog()
            Log.d(TAG, "restored instance state, syncing is $syncing")
        }
    }

    override fun onPostExecute(response: String) {
        Log.d(TAG, "onPostExecute: setting syncing to false and updating dialog")
        syncing = false
        updateSyncDialog(response)
    }

    override fun onPreExecute() {
        Log.d(TAG, "onPreExecute: setting syncing to true and show dialog")
        syncing = true
        showSyncDialog()
    }

    override fun onCancelled() {
        // Do nothing
    }

    override fun onProgressUpdate(percent: Int) {
        // Do nothing
    }

    fun createSyncDialog(){
        Log.d(TAG, "creating sync dialog")
        syncDialog = Dialog(this)
        syncDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        // Set the dialog
        syncDialog?.setCancelable(false)
        syncDialog?.setContentView(R.layout.sync_dialog)

        // Set up the button
        val button = syncDialog?.findViewById(R.id.sync_verification_button) as androidx.appcompat.widget.AppCompatButton
        button.setOnClickListener {
            syncDialog?.dismiss()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "saving instance state")
        super.onSaveInstanceState(outState)
        if (syncing == true) outState.putBoolean("syncing", true)
    }

    /*
    override fun onPause() {
        super.onPause()
        if (syncDialog != null){
            if (syncDialog?.isShowing!!)
                syncDialog?.dismiss()
        }
    }
    */

    // Shows a dialog to give progress feedback on synchronization
    fun showSyncDialog(){
        Log.d(TAG, "showing sync dialog")
        // Set the response
        val responseView = syncDialog?.findViewById(R.id.sync_response) as TextView
        responseView.setText(this.getString(R.string.executing_sync_notification))
        val progress = syncDialog?.findViewById(R.id.sync_progress) as ProgressBar
        /// Set the image feedback
        val imageFeedback = syncDialog?.findViewById(R.id.sync_feedback_image) as ImageView
        val button = syncDialog?.findViewById(R.id.sync_verification_button) as androidx.appcompat.widget.AppCompatButton

        // Show the updated views
        progress.visibility = View.VISIBLE
        imageFeedback.visibility = View.INVISIBLE
        button.visibility = View.GONE
        // Show the dialog
        syncDialog?.show()
    }

    // Updates the dialog showing progress on synchronization
    fun updateSyncDialog(response: String){
        Log.d(TAG, "updating sync dialog")
        val success = (response.equals(getString(R.string.valid_file_structure)))

        // Set the response
        val responseView = syncDialog?.findViewById(R.id.sync_response) as TextView
        responseView.setText(response)

        val progress = syncDialog?.findViewById(R.id.sync_progress) as ProgressBar

        /// Set the image feedback
        val imageFeedback = syncDialog?.findViewById(R.id.sync_feedback_image) as ImageView
        if (success) {
            imageFeedback.setImageResource(R.drawable.ic_check_green_40dp)
            responseView.setText(getString(R.string.executing_sync_complete))
        }
        else imageFeedback.setImageResource(R.drawable.ic_error_red_40dp)

        val button = syncDialog?.findViewById(R.id.sync_verification_button) as androidx.appcompat.widget.AppCompatButton

        // Show the updated views
        progress.visibility = View.INVISIBLE
        imageFeedback.visibility = View.VISIBLE
        button.visibility = View.VISIBLE
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    var mCallbacks: TaskCallbacks? = null
    var prefs: SharedPreferences? = null
    var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null


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

            // If the user enables manual file syncing give some info
            if (key.equals(getString(R.string.key_sync_allowed))){
                val isSyncAllowed = prefs.getBoolean(key, false)
                if (isSyncAllowed) showManualFileModificationDialog()
            }
        }

        // TODO (update) write unit test for file / database sync
        // Verify the user wants to sync files to the database
        syncPref?.setOnPreferenceClickListener{
            verifyImportProjectsDialog()
            true
        }
    }

    // Verifies the user wishes to sync files to the database
    fun verifyImportProjectsDialog(){
        val fragment = this
        val builder = AlertDialog.Builder(activity!!)
                .setTitle(R.string.warning)
                .setMessage(R.string.database_sync_warning)
                .setPositiveButton(R.string.ok) { _, _ ->
                    Log.d("settings activity", "Launching database sync asynct task")
                    // Execute the sync in the background
                    DatabaseSyncTask().execute(SettingsFragment.inputParams(fragment))
                }
                .setNegativeButton(R.string.cancel){_,_ -> }
                .setIcon(R.drawable.ic_warning_black_24dp)

        val dialog = builder.create()
        dialog.show()
    }

    // Shows dialog that gives info on manual file modification
    private fun showManualFileModificationDialog() {
        val builder = AlertDialog.Builder(activity!!)
                .setTitle(R.string.warning)
                .setMessage(R.string.file_modification_information)
                .setPositiveButton(R.string.ok) { _, _ ->  }
                .setIcon(R.drawable.ic_warning_black_24dp)

        val dialog = builder.create()
        dialog.show()
    }

    // This async task validates the files are in correct format then synchronizes to the database
    inner class DatabaseSyncTask : AsyncTask<inputParams, Void, String>() {

        // Do in background validates then executes import
        override fun doInBackground(vararg p0: inputParams): String {
            val fragment = p0.get(0).fragment
            val response = ProjectUtils.validateFileStructure(fragment.context)

            // If modified files are valid go ahead notify user of background work and import
            if (response.equals(fragment.getString(R.string.valid_file_structure))){
                ProjectUtils.importProjects(fragment.context)
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

    // These classes used for passing references through the async task
    class inputParams(val fragment: PreferenceFragmentCompat)
}
