package com.vwoom.timelapsegallery.activities

import android.app.Activity
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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.notification.NotificationUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils

class SettingsActivity : AppCompatActivity() {

    lateinit var prefs: SharedPreferences
    lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        lateinit var syncDialog: Dialog

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val syncPref: Preference? =  findPreference(getString(R.string.key_sync))

            // Listen for changes to shared preferences and update notification worker on change
            (activity as SettingsActivity).prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            (activity as SettingsActivity).prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
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

            // TODO (update) test file / database sync
            // TODO fix async task error on orientation change
            // Verify the user wants to sync files to the database
            syncPref?.setOnPreferenceClickListener{
                verifyImportProjectsDialog()
                true
            }
        }

        override fun onResume() {
            super.onResume()
            (activity as SettingsActivity).prefs.registerOnSharedPreferenceChangeListener(
                    (activity as SettingsActivity).prefListener
            )
        }

        override fun onPause() {
            super.onPause()
            (activity as SettingsActivity).prefs.unregisterOnSharedPreferenceChangeListener(
                    (activity as SettingsActivity).prefListener
            )
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

        // Shows a dialog to give progress feedback on synchronization
        fun showSyncDialog(){
            // Set the dialog
            syncDialog = Dialog(activity!!)
            syncDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            syncDialog.setCancelable(false)
            syncDialog.setContentView(R.layout.sync_dialog)

            // Set up the button
            val button = syncDialog.findViewById(R.id.sync_verification_button) as androidx.appcompat.widget.AppCompatButton
            button.setOnClickListener {
                syncDialog.dismiss()
            }

            // Show the dialog
            syncDialog.show()
        }

        // Updates the dialog showing progress on synchronization
        fun updateSyncDialog(response: String, success: Boolean){
            // Set the response
            val responseView = syncDialog.findViewById(R.id.sync_response) as TextView
            responseView.setText(response)

            val progress = syncDialog.findViewById(R.id.sync_progress) as ProgressBar

            /// Set the image feedback
            val imageFeedback = syncDialog.findViewById(R.id.sync_feedback_image) as ImageView
            if (success) {
                imageFeedback.setImageResource(R.drawable.ic_check_green_40dp)
                responseView.setText(getString(R.string.executing_sync_complete))
            }
            else imageFeedback.setImageResource(R.drawable.ic_error_red_40dp)

            val button = syncDialog.findViewById(R.id.sync_verification_button) as androidx.appcompat.widget.AppCompatButton

            // Show the updated views
            progress.visibility = View.INVISIBLE
            imageFeedback.visibility = View.VISIBLE
            button.visibility = View.VISIBLE
        }

        // Verifies the user wishes to sync files to the database
        fun verifyImportProjectsDialog(){
            val fragment = this
            val builder = AlertDialog.Builder(activity!!)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.database_sync_warning)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        Log.d("settings activity", "Importing projects")
                        showSyncDialog()
                        // Execute the sync in the background
                        databaseSyncTask().execute(inputParams(activity!!, fragment))
                    }
                    .setNegativeButton(R.string.cancel){_,_ -> }
                    .setIcon(R.drawable.ic_warning_black_24dp)

            val dialog = builder.create()
            dialog.show()
        }

        // This async task validates the files are in correct format then synchronizes to the database
        class databaseSyncTask : AsyncTask<inputParams, Void, outputParams>() {

            // Do in background validates then executes import
            override fun doInBackground(vararg p0: inputParams): outputParams {
                val context = p0.get(0).activity
                val fragment = p0.get(0).fragment
                val response = ProjectUtils.validateFileStructure(context)

                // If modified files are valid go ahead notify user of background work and import
                if (response.equals(context.getString(R.string.valid_file_structure))){
                    ProjectUtils.importProjects(context)
                }

                return outputParams(context, fragment, response)
            }

            // On post execute updates sync dialog
            override fun onPostExecute(result: outputParams) {
                super.onPostExecute(result)

                val context = result.activity
                val response = result.response
                val fragment = result.fragment

                val success: Boolean = response.equals(context.getString(R.string.valid_file_structure))
                (fragment as SettingsFragment).updateSyncDialog(response, success)
            }
        }

        // These classes used for passing references through the async task
        class inputParams(val activity: Activity, val fragment: PreferenceFragmentCompat)
        class outputParams(val activity: Activity, val fragment: PreferenceFragmentCompat, val response: String)
    }
}