package com.vwoom.timelapsegallery.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.utils.ProjectUtils

class SettingsViewModel : ViewModel() {

    var syncing: Boolean = false
    var showingSyncDialog: Boolean = false
    var showingFileModDialog: Boolean = false
    var showingVerifySyncDialog: Boolean = false
    lateinit var response: String

    suspend fun executeSync(context: Context) {
        response = ProjectUtils.validateFileStructure(context)
        if (response == context.getString(R.string.valid_file_structure)){
                ProjectUtils.importProjects(context)
            }
    }

}