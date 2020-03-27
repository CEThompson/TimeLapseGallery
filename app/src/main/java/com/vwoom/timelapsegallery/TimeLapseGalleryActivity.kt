package com.vwoom.timelapsegallery

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.vwoom.timelapsegallery.notification.NotificationUtils
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.widget.UpdateWidgetService

class TimeLapseGalleryActivity : AppCompatActivity() {

    lateinit var interstitialAd: InterstitialAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_lapse_gallery)

        // Initialize mobile ads
        MobileAds.initialize(this) { }
        interstitialAd = InterstitialAd(this)
        interstitialAd.adUnitId = getString(R.string.interstitial_ad_unit_id) // get the interstitial id for the current build variant debug or release
    }

    /* Ensure deletion of temporary photo files */
    override fun onDestroy() {
        super.onDestroy()
        FileUtils.deleteTempFiles(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        UpdateWidgetService.startActionUpdateWidgets(this)
        NotificationUtils.scheduleNotificationWorker(this)
    }

}
