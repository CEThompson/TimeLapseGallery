package com.vwoom.timelapsegallery.settings

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.notification.NotificationUtils
import com.vwoom.timelapsegallery.utils.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.async

// TODO fix shared preference options and dialog content
class SettingsFragment : PreferenceFragmentCompat() {
    private var prefs: SharedPreferences? = null
    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    // Dialogs
    private var mSyncDialog: Dialog? = null
    private var mFileModDialog: Dialog? = null
    private var mVerifySyncDialog: Dialog? = null

    private val settingsViewModel: SettingsViewModel by viewModels {
        InjectorUtils.provideSettingsViewModelFactory()
    }

    private var databaseSyncJob: Job? = null

    // TODO remove firebase analytics?
    /* Analytics */
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    override fun onDestroy() {
        super.onDestroy()
        databaseSyncJob?.cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up dialogs
        createSyncDialog()
        createVerifyProjectImportDialog()
        createManualFileModificationDialog()

        // Restore dialog state from view model
        if (settingsViewModel.syncing) {
            executeSync()
        }

        if (settingsViewModel.showingSyncDialog) {
            updateSyncDialog(settingsViewModel.response)
            mSyncDialog?.show()
        }

        if (settingsViewModel.showingFileModDialog) showFileModificationDialog()
        if (settingsViewModel.showingVerifySyncDialog) showVerifyProjectImportDialog()

        // TODO remove firebase?
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

            // TODO remove ads from application
            if (key == this.getString(R.string.key_ads_disabled)){
                val adsDisabled = prefs.getBoolean(context?.getString(R.string.key_ads_disabled), false)
                if (adsDisabled)
                    mFirebaseAnalytics?.logEvent(context?.getString(R.string.analytics_ads_disabled)!!, null)
            }

            // If the user changes the notifications enabled preference trigger the notification worker to update any alarms
            if (key == this.getString(R.string.key_notifications_enabled)) {
                NotificationUtils.scheduleNotificationWorker(requireContext())

                // Log notifications
                val notificationsEnabled = prefs.getBoolean(activity?.getString(R.string.key_notifications_enabled), true)
                if (!notificationsEnabled)
                    mFirebaseAnalytics?.logEvent(context?.getString(R.string.analytics_notifications_disabled)!!, null)
            }

            // Same for notification time
            if (key == this.getString(R.string.key_notification_time)) {
                NotificationUtils.scheduleNotificationWorker(requireContext())

                // Log notification time selection
                val notificationTime = prefs.getString(activity?.getString(R.string.key_notification_time), getString(R.string.notification_time_default))
                val params = Bundle()
                params.putString(context?.getString(R.string.analytics_notification_time)!!, notificationTime)
                mFirebaseAnalytics?.logEvent(context?.getString(R.string.analytics_select_notification_time)!!, params)
            }

            // Track playback interval selection
            if (key == getString(R.string.key_playback_interval)){
                val interval = prefs.getString(getString(R.string.key_playback_interval), getString(R.string.playback_interval_default));
                val params = Bundle()
                params.putString(context?.getString(R.string.analytics_playback_interval)!!, interval)
                mFirebaseAnalytics?.logEvent(context?.getString(R.string.analytics_select_playback_interval)!!, params)
            }

            // If the user enables manual file mSyncing give some info
            if (key == getString(R.string.key_sync_allowed)){
                val isSyncAllowed = prefs.getBoolean(key, false)
                if (isSyncAllowed) {
                    showFileModificationDialog()
                    // Log manual syncing
                    mFirebaseAnalytics?.logEvent(context?.getString(R.string.analytics_manual_sync_enabled)!!, null)
                }
            }
        }

        // Verify the user wants to sync files to the database
        syncPref?.setOnPreferenceClickListener{
            showVerifyProjectImportDialog()
            true
        }
    }

    //
    // Start Dialog Functions
    //
    private fun createVerifyProjectImportDialog(){
        val builder = AlertDialog.Builder(requireContext())
                .setTitle(R.string.warning)
                .setMessage(R.string.database_sync_warning)
                .setPositiveButton(R.string.ok) { _, _ ->
                    Log.d("settings activity", "Launching database sync async task")
                    settingsViewModel.showingVerifySyncDialog = false
                    executeSync()
                }
                .setNegativeButton(R.string.cancel){_,_ -> settingsViewModel.showingVerifySyncDialog = false}
                .setIcon(R.drawable.ic_warning_black_24dp)
        mVerifySyncDialog = builder.create()
    }
    private fun createManualFileModificationDialog() {
        val builder = AlertDialog.Builder(requireContext())
                .setTitle(R.string.warning)
                .setMessage(R.string.file_modification_information)
                .setPositiveButton(R.string.ok) { _, _ -> settingsViewModel.showingFileModDialog = false}
                .setIcon(R.drawable.ic_warning_black_24dp)
        mFileModDialog = builder.create()
    }
    private fun createSyncDialog(){
        Log.d(TAG, "creating sync dialog")
        mSyncDialog = Dialog(requireContext())
        mSyncDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        // Set the dialog
        mSyncDialog?.setCancelable(false)
        mSyncDialog?.setContentView(R.layout.dialog_sync)

        // Set up the button
        val button = mSyncDialog?.findViewById(R.id.sync_verification_button) as androidx.appcompat.widget.AppCompatButton
        button.setOnClickListener {
            mSyncDialog?.dismiss()
            settingsViewModel.showingSyncDialog = false
        }
    }
    // Shows a dialog to give progress feedback on synchronization
    private fun showSyncDialog(){
        Log.d(TAG, "showing sync dialog")
        // Set the response
        val responseView = mSyncDialog?.findViewById(R.id.sync_response) as TextView
        responseView.text = this.getString(R.string.executing_sync_notification)
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
    private fun updateSyncDialog(result: ValidationResult<Nothing>){
        Log.d(TAG, "updating sync dialog")
        // Set the response
        val responseView = mSyncDialog?.findViewById(R.id.sync_response) as TextView
        val progress = mSyncDialog?.findViewById(R.id.sync_progress) as ProgressBar
        val imageFeedback = mSyncDialog?.findViewById(R.id.sync_feedback_image) as ImageView
        val button = mSyncDialog?.findViewById(R.id.sync_verification_button) as androidx.appcompat.widget.AppCompatButton

        imageFeedback.setImageResource(R.drawable.ic_error_red_40dp)
        when(result){
            is ValidationResult.Error.NoFilesError -> {
                responseView.text = requireContext()
                        .getString(R.string.no_files_in_directory_error,
                                result.directoryUrl)
            }
            is ValidationResult.Error.InvalidCharacterError -> {
                responseView.text = requireContext()
                        .getString(R.string.invalid_character_error,
                        result.projectName,
                        RESERVED_CHARACTERS)
            }
            is ValidationResult.Error.DuplicateIdError -> {
                responseView.text = requireContext().getString(R.string.duplicate_id_error, result.projectName)
            }
            is ValidationResult.Error.InvalidPhotoFileError -> {
                responseView.text = requireContext()
                        .getString(R.string.invalid_photo_file_error,
                                result.photoUrl,
                                result.projectName)
            }
            is ValidationResult.Error.InvalidFolder -> {
                responseView.text = getString(R.string.invalid_folder_error,
                        result.url,
                        result.exception?.localizedMessage)
            }
            is ValidationResult.Success -> {
                imageFeedback.setImageResource(R.drawable.ic_check_green_40dp)
                responseView.text = getString(R.string.executing_sync_complete)
            }
        }

        // Show the updated views
        progress.visibility = View.INVISIBLE
        imageFeedback.visibility = View.VISIBLE
        button.visibility = View.VISIBLE
    }
    private fun showVerifyProjectImportDialog() {
        settingsViewModel.showingVerifySyncDialog = true
        mVerifySyncDialog?.show()
    }
    private fun showFileModificationDialog() {
        settingsViewModel.showingFileModDialog = true
        mFileModDialog?.show()
    }
    //
    // End Dialog Functions
    //

    // TODO find way to update status of project import
    private fun executeSync(){
        showSyncDialog()
        databaseSyncJob = settingsViewModel.viewModelScope.async {
            settingsViewModel.executeSync(requireContext())
            updateSyncDialog(settingsViewModel.response)
        }
    }

    companion object {
        private val TAG = SettingsFragment::class.java.simpleName
    }
}
