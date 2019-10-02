package com.vwoom.timelapsegallery.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.notification.NotificationUtils

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

            // TODO (update) implement project import button here
            //val syncButton: Preference? =  findPreference(getString(R.string.key_sync))

            // Listen for changes to shared preferences and update notification worker on change
            (activity as SettingsActivity).prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            (activity as SettingsActivity).prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                Log.d("settings activity", "Notification listener activitating for key = $key")
                if (key.equals(this.getString(R.string.key_notifications_enabled)))
                    NotificationUtils.scheduleNotificationWorker(activity);
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
    }
}