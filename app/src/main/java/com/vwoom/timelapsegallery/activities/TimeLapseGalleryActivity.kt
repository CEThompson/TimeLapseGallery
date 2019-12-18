package com.vwoom.timelapsegallery.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import butterknife.ButterKnife
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.utils.FileUtils

// TODO refactor app to use one activity with multiple fragments paradigm
// TODO convert activities

class TimeLapseGalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_lapse_gallery)

        ButterKnife.bind(this)
        val toolbar: Toolbar = findViewById(R.id.main_activity_toolbar)
        setSupportActionBar(toolbar)

        // Set the icon for the toolbar
        if (getSupportActionBar() != null) getSupportActionBar()?.setIcon(R.drawable.actionbar_space_between_icon_and_title)

        // Initialize mobile ads
        MobileAds.initialize(this, OnInitializationCompleteListener { initializationStatus: InitializationStatus? -> })
    }

    /* Ensure deletion of temporary photo files */
    override fun onDestroy() {
        super.onDestroy()
        Log.d("deletion check", "onStop newProjectActivity firing")
        FileUtils.deleteTempFiles(this) // Make sure to clean up temporary files
    }

    // TODO (update) create menu option to show todays projects
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val intent = Intent(this@TimeLapseGalleryActivity, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
