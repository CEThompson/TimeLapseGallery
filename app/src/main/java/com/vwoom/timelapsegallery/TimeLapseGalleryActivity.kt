package com.vwoom.timelapsegallery

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.vwoom.timelapsegallery.notification.NotificationUtils
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.widget.UpdateWidgetService

class TimeLapseGalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_lapse_gallery)
    }

    /* Ensure deletion of temporary photo files */
    override fun onDestroy() {
        super.onDestroy()
        FileUtils.deleteTempFiles(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        UpdateWidgetService.startActionUpdateWidgets(this)
        NotificationUtils.scheduleNotificationWorker(this)
    }

}
