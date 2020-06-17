package com.vwoom.timelapsegallery

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.vwoom.timelapsegallery.notification.NotificationUtils
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.widget.UpdateWidgetService

// TODO: consider implementing left handed and right handed modes
// TODO: implement content grouping where appropriate for accessibility
// TODO: consider any content descriptions that need dynamic content descriptions with live regions
// TODO: consider implement search tag filters as chips instead of checkboxes

class TimeLapseGalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_lapse_gallery)
    }

    // On exit delete temp files and update notifications and widgets
    override fun onDestroy() {
        super.onDestroy()
        FileUtils.deleteTempFiles(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        UpdateWidgetService.startActionUpdateWidgets(this)
        NotificationUtils.scheduleNotificationWorker(this)
    }

}
