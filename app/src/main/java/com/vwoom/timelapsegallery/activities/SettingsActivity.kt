package com.vwoom.timelapsegallery.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
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
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val syncPref: Preference? =  findPreference(getString(R.string.key_sync))

            // Listen for changes to shared preferences and update notification worker on change
            (activity as SettingsActivity).prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            (activity as SettingsActivity).prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                Log.d("settings activity", "Notification listener activitating for key = $key")
                if (key.equals(this.getString(R.string.key_notifications_enabled))) {
                    NotificationUtils.scheduleNotificationWorker(activity);
                }
                if (key.equals(this.getString(R.string.key_notification_time))) {
                    NotificationUtils.scheduleNotificationWorker(activity)
                }
                if (key.equals(getString(R.string.key_sync_allowed))){
                    val isSyncAllowed = prefs.getBoolean(key, false)
                    if (isSyncAllowed) showManualFileModificationDialog()
                }
            }

            // TODO (update) test file / database sync
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

        private fun showManualFileModificationDialog() {
            val builder = AlertDialog.Builder(activity!!)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.file_modification_information)
                    .setPositiveButton(R.string.ok) { _, _ ->  }
                    .setIcon(R.drawable.ic_warning_black_24dp)

            val dialog = builder.create()
            dialog.show()
        }

        fun verifyImportProjectsDialog(){
            val builder = AlertDialog.Builder(activity!!)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.database_sync_warning)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        Log.d("settings activity", "Importing projects")

                        // Execute the sync in the background
                        databaseSyncTask().execute(inputParams(context!!))
                    }
                    .setNegativeButton(R.string.cancel){_,_ -> }
                    .setIcon(R.drawable.ic_warning_black_24dp)

            val dialog = builder.create()
            dialog.show()
        }
    }

    class databaseSyncTask : AsyncTask<inputParams, Void, outputParams>() {
        override fun doInBackground(vararg p0: inputParams): outputParams {
            val context = p0.get(0).context
            val response = ProjectUtils.validateFileStructure(context)

            // If modified files are valid go ahead notify user of background work and import
            if (response.equals(context.getString(R.string.valid_file_structure))){
                Toast.makeText(context, context.getString(R.string.executing_sync_notification), Toast.LENGTH_LONG).show()
                ProjectUtils.importProjects(context)
            }

            return outputParams(context, response)
        }

        override fun onPostExecute(result: outputParams) {
            super.onPostExecute(result)

            val context = result.context
            val response = result.response

            // If the file structure was valid finishing task notify the user
            if (response.equals(context.getString(R.string.valid_file_structure)))
                Toast.makeText(context, context.getString(R.string.executing_sync_complete), Toast.LENGTH_LONG).show()
            // Otherwise notify the user of the error response
            else
                Toast.makeText(context, response, Toast.LENGTH_LONG).show()

        }
    }

    class inputParams(val context: Context)
    class outputParams(val context: Context, val response: String)
}