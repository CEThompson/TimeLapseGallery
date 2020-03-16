package com.vwoom.timelapsegallery.settings

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.utils.FILE_VALIDATION_RESPONSE_WAITING
import com.vwoom.timelapsegallery.utils.ProjectUtils
import com.vwoom.timelapsegallery.utils.VALID_DIRECTORY_STRUCTURE

class SettingsViewModel : ViewModel() {

    var syncing: Boolean = false
    var showingSyncDialog: Boolean = false
    var showingFileModDialog: Boolean = false
    var showingVerifySyncDialog: Boolean = false
    var response: String = FILE_VALIDATION_RESPONSE_WAITING

    suspend fun executeSync(context: Context) {
        response = FILE_VALIDATION_RESPONSE_WAITING

        val externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        // Validate the directory if we have a directory
        if (externalFilesDir!= null) {
            response = ProjectUtils.validateFileStructure(
                    externalFilesDir)
        }
        // If the directory is valid import projects
        if (response == VALID_DIRECTORY_STRUCTURE){
                syncing = true
                ProjectUtils.importProjects(context)
                syncing = false
        }
    }

}