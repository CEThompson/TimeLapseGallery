package com.vwoom.timelapsegallery

import android.os.Bundle
import android.os.Environment
import com.vwoom.timelapsegallery.di.activity.BaseActivity
import com.vwoom.timelapsegallery.notification.NotificationUtils
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.widget.UpdateWidgetService

// TODO (deferred): implement left handed and right handed modes
// TODO (deferred): implement content grouping where appropriate for accessibility
// TODO (deferred): consider any content descriptions that need dynamic content descriptions with live regions
// TODO (1.3): consider implement search tag filters as chips instead of checkboxes
class TimeLapseGalleryActivity : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
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
