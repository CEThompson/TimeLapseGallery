package com.vwoom.timelapsegallery

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.vwoom.timelapsegallery.notification.NotificationUtils
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.widget.UpdateWidgetService
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

// TODO (deferred): implement left handed and right handed modes
// TODO (deferred): implement content grouping where appropriate for accessibility
// TODO (deferred): consider any content descriptions that need dynamic content descriptions with live regions
// TODO (1.3): consider implement search tag filters as chips instead of checkboxes
class TimeLapseGalleryActivity : AppCompatActivity(), HasAndroidInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

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
