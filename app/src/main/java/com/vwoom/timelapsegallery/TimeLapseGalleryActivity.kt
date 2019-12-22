package com.vwoom.timelapsegallery

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.vwoom.timelapsegallery.utils.FileUtils

class TimeLapseGalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_lapse_gallery)

        // Initialize mobile ads
        MobileAds.initialize(this, OnInitializationCompleteListener { initializationStatus: InitializationStatus? -> })
    }

    /* Ensure deletion of temporary photo files */
    override fun onDestroy() {
        super.onDestroy()
        Log.d("deletion check", "onStop newProjectActivity firing")
        FileUtils.deleteTempFiles(this) // Make sure to clean up temporary files
    }
}