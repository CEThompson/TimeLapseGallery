package com.vwoom.timelapsegallery.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vwoom.timelapsegallery.R

// TODO refactor app to use one activity with multiple fragments paradigm
// TODO convert activities
// TODO (1) AddPhotoActivity -> AddPhotoFragment
// TODO (2) CameraActivity -> CameraFragment
// TODO (3) DetailsActivity -> DetailFragment
// TODO (4) MainActivity -> GalleryFragment
// TODO (5) NewProjectActivity -> EditFragment?
// TODO (6) SettingsActivity -> SettingsFragment

class TimeLapseGalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_lapse_gallery)
    }
}
