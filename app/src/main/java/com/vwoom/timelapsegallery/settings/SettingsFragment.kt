package com.vwoom.timelapsegallery.settings

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.di.viewmodel.ViewModelFactory
import com.vwoom.timelapsegallery.di.base.BasePreferenceFragment
import com.vwoom.timelapsegallery.gif.GifUtils
import com.vwoom.timelapsegallery.notification.NotificationUtils
import com.vwoom.timelapsegallery.utils.ImportUtils
import com.vwoom.timelapsegallery.utils.RESERVED_CHARACTERS
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import timber.log.Timber
import javax.inject.Inject

// TODO (deferred): consider allowing for adjustment of gallery columns
class SettingsFragment : BasePreferenceFragment() {
    private var prefs: SharedPreferences? = null
    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private val settingsViewModel: SettingsViewModel by viewModels {
        viewModelFactory
    }

    // Dialogs
    private var syncDialog: Dialog? = null
    private var fileModDialog: Dialog? = null
    private var verifySyncDialog: Dialog? = null

    // For syncing database to files
    private var databaseSyncJob: Job? = null

    private var firebaseAnalytics: FirebaseAnalytics? = null

    override fun onDestroy() {
        super.onDestroy()
        databaseSyncJob?.cancel()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        super.onCreate(savedInstanceState)
        // Set up dialogs
        createSyncDialog()
        createVerifyProjectImportDialog()
        createManualFileModificationDialog()
        firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore dialog state from view model
        if (settingsViewModel.syncing) {
            executeSync()   // if syncing continue
        }
        if (settingsViewModel.showingSyncDialog) {
            updateSyncDialog(settingsViewModel.response)
            syncDialog?.show()
        }
        if (settingsViewModel.showingFileModDialog) showFileModificationDialog()
        if (settingsViewModel.showingVerifySyncDialog) showVerifyProjectImportDialog()
        setupViewModel()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    override fun onDetach() {
        super.onDetach()
        firebaseAnalytics = null
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

        val syncPref: Preference? = findPreference(getString(R.string.key_sync))

        // Listen for changes to shared preferences and update notification worker on change
        prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            Timber.d("Notification listener activating for key = $key")

            if (key == this.getString(R.string.key_gif_auto_convert)) {
                val autoConvert = prefs.getBoolean(key, true)
                // If auto convert disabled cancel any gif workers
                if (!autoConvert) GifUtils.cancelGifWorker(requireContext())
                // NOTE: if enabled gif workers will only trigger when projects with gifs have photos added
                // Therefore do not trigger a gif workers when this setting is enabled
            }

            // If the user changes the notifications enabled preference trigger the notification worker to update any alarms
            if (key == this.getString(R.string.key_notifications_enabled)) {
                NotificationUtils.scheduleNotificationWorker(requireContext())

                // Log notifications
                val notificationsEnabled = prefs.getBoolean(activity?.getString(R.string.key_notifications_enabled), true)
                if (!notificationsEnabled)
                    firebaseAnalytics?.logEvent(requireContext().getString(R.string.analytics_notifications_disabled), null)
            }

            if (key == this.getString(R.string.key_notification_time)) {
                NotificationUtils.scheduleNotificationWorker(requireContext())

                // Log notification time selection
                val notificationTime = prefs.getString(activity?.getString(R.string.key_notification_time), getString(R.string.notification_time_default))
                val params = Bundle()
                params.putString(context?.getString(R.string.analytics_notification_time)!!, notificationTime)
                firebaseAnalytics?.logEvent(requireContext().getString(R.string.analytics_notification_time), params)
            }

            // Track playback interval selection
            if (key == getString(R.string.key_playback_interval)) {
                val interval = prefs.getString(getString(R.string.key_playback_interval), getString(R.string.playback_interval_default))
                val params = Bundle()
                params.putString(context?.getString(R.string.analytics_playback_interval)!!, interval)
                firebaseAnalytics?.logEvent(requireContext().getString(R.string.analytics_playback_interval), params)
            }

            // If the user enables manual file mSyncing give some info
            if (key == getString(R.string.key_sync_allowed)) {
                val isSyncAllowed = prefs.getBoolean(key, false)
                if (isSyncAllowed) {
                    showFileModificationDialog()
                    // Log manual syncing
                    firebaseAnalytics?.logEvent(requireContext().getString(R.string.analytics_manual_sync_enabled), null)
                }
            }

            if (key == requireContext().getString(R.string.key_schedule_display)) {
                val schedulesDisplayed = prefs.getBoolean(key, true)
                if (!schedulesDisplayed) {
                    firebaseAnalytics?.logEvent(requireContext().getString(R.string.analytics_schedule_displays_disabled), null)
                }
            }
        }

        // Verify the user wants to sync files to the database
        syncPref?.setOnPreferenceClickListener {
            showVerifyProjectImportDialog()
            true
        }
    }

    private fun createVerifyProjectImportDialog() {
        val builder = AlertDialog.Builder(requireContext())
                .setTitle(R.string.warning)
                .setMessage(R.string.database_sync_warning)
                .setPositiveButton(R.string.ok) { _, _ ->
                    Timber.d("Launching database sync async task")
                    settingsViewModel.showingVerifySyncDialog = false
                    executeSync()
                }
                .setNegativeButton(R.string.cancel) { _, _ -> settingsViewModel.showingVerifySyncDialog = false }
                .setIcon(R.drawable.ic_warning_black_24dp)
        verifySyncDialog = builder.create()
    }

    private fun createManualFileModificationDialog() {
        val builder = AlertDialog.Builder(requireContext())
                .setTitle(R.string.warning)
                .setMessage(R.string.file_modification_information)
                .setPositiveButton(R.string.ok) { _, _ -> settingsViewModel.showingFileModDialog = false }
                .setIcon(R.drawable.ic_warning_black_24dp)
        fileModDialog = builder.create()
    }

    private fun createSyncDialog() {
        Timber.d("creating sync dialog")
        syncDialog = Dialog(requireContext())
        syncDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        // Set the dialog
        syncDialog?.setCancelable(false)
        syncDialog?.setContentView(R.layout.dialog_sync)

        // Set up the button
        val button = syncDialog?.findViewById(R.id.sync_verification_button) as androidx.appcompat.widget.AppCompatButton
        button.setOnClickListener {
            syncDialog?.dismiss()
            settingsViewModel.showingSyncDialog = false
        }
    }

    // Shows a dialog to give progress feedback on synchronization
    private fun showSyncDialog() {
        Timber.d("showing sync dialog")
        // Set the response
        val responseView = syncDialog?.findViewById(R.id.sync_response) as TextView
        responseView.text = this.getString(R.string.executing_sync_notification)
        val overallProgress = syncDialog?.findViewById(R.id.overall_sync_progress) as ProgressBar
        val projectProgress = syncDialog?.findViewById(R.id.project_sync_progress) as ProgressBar
        val photoProgress = syncDialog?.findViewById(R.id.photo_sync_progress) as ProgressBar
        val projectProgressTv = syncDialog?.findViewById(R.id.project_sync_tv) as TextView
        val photoProgressTv = syncDialog?.findViewById(R.id.photo_sync_tv) as TextView
        /// Set the image feedback
        val imageFeedback = syncDialog?.findViewById(R.id.sync_feedback_image) as ImageView
        val okButton = syncDialog?.findViewById(R.id.sync_verification_button) as androidx.appcompat.widget.AppCompatButton

        // Show the updated views
        overallProgress.visibility = VISIBLE
        projectProgress.visibility = VISIBLE
        projectProgressTv.visibility = VISIBLE
        photoProgress.visibility = VISIBLE
        photoProgressTv.visibility = VISIBLE
        imageFeedback.visibility = GONE
        okButton.visibility = GONE
        // Show the dialog
        syncDialog?.show()
    }

    // Updates the dialog showing progress on synchronization
    private fun updateSyncDialog(result: ValidationResult<List<ImportUtils.ProjectDataBundle>>) {
        Timber.d("updating sync dialog")
        // Set the response
        val responseView = syncDialog?.findViewById(R.id.sync_response) as TextView
        val overallProgress = syncDialog?.findViewById(R.id.overall_sync_progress) as ProgressBar
        val projectProgress = syncDialog?.findViewById(R.id.project_sync_progress) as ProgressBar
        val projectProgressTv = syncDialog?.findViewById(R.id.project_sync_tv) as TextView
        val photoProgress = syncDialog?.findViewById(R.id.photo_sync_progress) as ProgressBar
        val photoProgressTv = syncDialog?.findViewById(R.id.photo_sync_tv) as TextView
        val imageFeedback = syncDialog?.findViewById(R.id.sync_feedback_image) as ImageView
        val button = syncDialog?.findViewById(R.id.sync_verification_button) as androidx.appcompat.widget.AppCompatButton

        imageFeedback.setImageResource(R.drawable.ic_error_red_40dp)
        when (result) {
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
            is ValidationResult.InProgress -> {
                // Do nothing
                // Note: progress updates handled by observables in setupViewModel()
            }
        }
        // Show the updated views
        overallProgress.visibility = GONE
        projectProgress.visibility = GONE
        projectProgressTv.visibility = GONE
        photoProgress.visibility = GONE
        photoProgressTv.visibility = GONE
        imageFeedback.visibility = VISIBLE
        button.visibility = VISIBLE
    }

    private fun showVerifyProjectImportDialog() {
        settingsViewModel.showingVerifySyncDialog = true
        verifySyncDialog?.show()
    }

    private fun showFileModificationDialog() {
        settingsViewModel.showingFileModDialog = true
        fileModDialog?.show()
    }

    private fun executeSync() {
        showSyncDialog()
        databaseSyncJob = settingsViewModel.viewModelScope.async {
            // Lock screen orientation during sync
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            settingsViewModel.executeSync(requireContext())
            updateSyncDialog(settingsViewModel.response)
            // Restore screen orientation
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            // Log sync to analytics
            firebaseAnalytics?.logEvent(getString(R.string.analytics_manual_sync_executed), null)
        }
    }

    private fun setupViewModel() {
        // Updates the progress of importing projects
        settingsViewModel.projectProgress.observe(viewLifecycleOwner, {
            val progress = syncDialog?.findViewById<ProgressBar>(R.id.project_sync_progress)
            progress?.progress = it + 1
            val tv = syncDialog?.findViewById<TextView>(R.id.project_sync_tv)
            tv?.text = getString(R.string.project_sync_text,
                    it + 1,
                    SyncProgressCounter.projectMax.value!! + 1)
        })
        // Updates the max number of projects to import
        settingsViewModel.projectMax.observe(viewLifecycleOwner, {
            val progress = syncDialog?.findViewById<ProgressBar>(R.id.project_sync_progress)
            progress?.max = it + 1
        })
        // Updates the progress of photos imported for a project
        settingsViewModel.photoProgress.observe(viewLifecycleOwner, {
            val progress = syncDialog?.findViewById<ProgressBar>(R.id.photo_sync_progress)
            progress?.progress = it + 1
            val tv = syncDialog?.findViewById<TextView>(R.id.photo_sync_tv)
            tv?.text = getString(R.string.photo_sync_text,
                    SyncProgressCounter.projectProgress.value!! + 1,
                    it + 1,
                    SyncProgressCounter.projectMax.value!! + 1)
        })
        // Updates the photo maximum import for a project
        settingsViewModel.photoMax.observe(viewLifecycleOwner, {
            val progress = syncDialog?.findViewById<ProgressBar>(R.id.photo_sync_progress)
            progress?.max = it + 1
        })

    }
}
