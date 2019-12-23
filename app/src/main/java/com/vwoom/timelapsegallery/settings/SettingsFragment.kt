package com.vwoom.timelapsegallery.settings

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
import com.google.firebase.analytics.FirebaseAnalytics
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.notification.NotificationUtils

class SettingsFragment : PreferenceFragmentCompat() {
    var prefs: SharedPreferences? = null
    var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    // Dialogs
    var mSyncDialog: Dialog? = null
    var mFileModDialog: Dialog? = null
    var mVerifySyncDialog: Dialog? = null

    // State
    var mSyncing: Boolean? = null
    var mShowingSyncDialog: Boolean? = null
    var mShowingFileModDialog: Boolean? = null
    var mShowingVerifySyncDialog: Boolean? = null
    var mResponse: String? = null

    /* Analytics */
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO handle database sync in coroutine or work manager

        // TODO handle action bar
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set up dialogs
        createSyncDialog()
        createVerifyProjectImportDialog()
        createManualFileModificationDialog()

        // TODO drive settings fragment from view model?
        // Restore instance state of sync dialog
        if (savedInstanceState != null) {
            mSyncing = savedInstanceState.getBoolean("mSyncing", false)
            mShowingSyncDialog = savedInstanceState.getBoolean("mShowingSyncDialog", false)
            mShowingFileModDialog = savedInstanceState.getBoolean("mShowingFileModDialog", false)
            mShowingVerifySyncDialog = savedInstanceState.getBoolean("mShowingVerifySyncDialog", false)
            mResponse = savedInstanceState.getString("mResponse", null)

            if (mSyncing == true) showSyncDialog()
            else if (mShowingSyncDialog == true) {
                updateSyncDialog(mResponse!!)
                mSyncDialog?.show()
            }
            else if (mShowingFileModDialog == true) showFileModificationDialog()
            else if (mShowingVerifySyncDialog == true) showVerifyProjectImportDialog()
        }

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    override fun onDetach() {
        super.onDetach()
        mFirebaseAnalytics = null
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

            if (key.equals(this.getString(R.string.key_ads_disabled))){
                val adsDisabled = prefs.getBoolean(context?.getString(R.string.key_ads_disabled), false)
                if (adsDisabled)
                    mFirebaseAnalytics?.logEvent(context?.getString(R.string.analytics_ads_disabled)!!, null)
            }

            // If the user changes the notifications enabled preference trigger the notification worker to update any alarms
            if (key.equals(this.getString(R.string.key_notifications_enabled))) {
                NotificationUtils.scheduleNotificationWorker(activity);

                // Log notifications
                val notificationsEnabled = prefs.getBoolean(activity?.getString(R.string.key_notifications_enabled), true)
                if (!notificationsEnabled)
                    mFirebaseAnalytics?.logEvent(context?.getString(R.string.analytics_notifications_disabled)!!, null)
            }

            // Same for notification time
            if (key.equals(this.getString(R.string.key_notification_time))) {
                NotificationUtils.scheduleNotificationWorker(activity)

                // Log notification time selection
                val notificationTime = prefs.getString(activity?.getString(R.string.key_notification_time), "7")
                val params = Bundle()
                params.putString(context?.getString(R.string.analytics_notification_time)!!, notificationTime)
                mFirebaseAnalytics?.logEvent(context?.getString(R.string.analytics_select_notification_time)!!, params)
            }

            // Track playback interval selection
            if (key.equals(getString(R.string.key_playback_interval))){
                val interval = prefs.getString(getString(R.string.key_playback_interval), "50");
                val params = Bundle()
                params.putString(context?.getString(R.string.analytics_playback_interval)!!, interval)
                mFirebaseAnalytics?.logEvent(context?.getString(R.string.analytics_select_playback_interval)!!, params)
            }

            // If the user enables manual file mSyncing give some info
            if (key.equals(getString(R.string.key_sync_allowed))){
                val isSyncAllowed = prefs.getBoolean(key, false)
                if (isSyncAllowed) {
                    showFileModificationDialog()
                    // Log manual syncing
                    mFirebaseAnalytics?.logEvent(context?.getString(R.string.analytics_manual_sync_enabled)!!, null)
                }
            }
        }

        // TODO (update) write unit test for file / database sync
        // Verify the user wants to sync files to the database
        syncPref?.setOnPreferenceClickListener{
            showVerifyProjectImportDialog()
            true
        }
    }

    /* Dialog functions */
    fun createVerifyProjectImportDialog(){
        val builder = AlertDialog.Builder(requireContext())
                .setTitle(R.string.warning)
                .setMessage(R.string.database_sync_warning)
                .setPositiveButton(R.string.ok) { _, _ ->
                    Log.d("settings activity", "Launching database sync asynct task")
                    // Execute the sync in the background
                    // TODO execute database sync with coroutine or work manager
                    // executeDatabaseSync()
                    mShowingVerifySyncDialog = false
                }
                .setNegativeButton(R.string.cancel){_,_ -> mShowingVerifySyncDialog = false}
                .setIcon(R.drawable.ic_warning_black_24dp)

        mVerifySyncDialog = builder.create()
    }
    fun createManualFileModificationDialog() {
        val builder = AlertDialog.Builder(requireContext())
                .setTitle(R.string.warning)
                .setMessage(R.string.file_modification_information)
                .setPositiveButton(R.string.ok) { _, _ -> mShowingFileModDialog = false}
                .setIcon(R.drawable.ic_warning_black_24dp)
        mFileModDialog = builder.create()
    }
    fun createSyncDialog(){
        Log.d(TAG, "creating sync dialog")
        mSyncDialog = Dialog(requireContext())
        mSyncDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        // Set the dialog
        mSyncDialog?.setCancelable(false)
        mSyncDialog?.setContentView(R.layout.sync_dialog)

        // Set up the button
        val button = mSyncDialog?.findViewById(R.id.sync_verification_button) as androidx.appcompat.widget.AppCompatButton
        button.setOnClickListener {
            mSyncDialog?.dismiss()
            mShowingSyncDialog = false
        }
    }
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
    // TODO convert these callbacks to coroutine callbacks or work manager callbacks?
    /*
    override fun onPostExecute(response: String) {
        Log.d(TAG, "onPostExecute: setting mSyncing to false and updating dialog")
        mResponse = response
        mSyncing = false
        updateSyncDialog(response)

        // Log syncing and responses
        val params = Bundle()
        params.putString(getString(R.string.analytics_sync_response), response)
        mFirebaseAnalytics?.logEvent(getString(R.string.analytics_manual_sync_executed), params)
    }
    override fun onPreExecute() {
        Log.d(TAG, "onPreExecute: setting mSyncing to true and show dialog")
        mSyncing = true
        showSyncDialog()
        mShowingSyncDialog = true
    }*/

    /* Settings Fragment Callbacks */
    fun showVerifyProjectImportDialog() {
        mShowingVerifySyncDialog = true
        mVerifySyncDialog?.show()
    }
    fun showFileModificationDialog() {
        mShowingFileModDialog = true
        mFileModDialog?.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mSyncing == true) outState.putBoolean("mSyncing", true)
        if (mShowingFileModDialog == true) outState.putBoolean("mShowingFileModDialog", true)
        if (mShowingVerifySyncDialog == true) outState.putBoolean("mShowingVerifySyncDialog", true)
        if (mShowingSyncDialog == true) outState.putBoolean("mShowingSyncDialog", true)
        if (mResponse != null) outState.putString("mResponse", mResponse)
    }

    companion object {
        private val TAG = SettingsFragment::class.java.simpleName
    }
}
