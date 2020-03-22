package com.vwoom.timelapsegallery.settings

import android.content.Context
import android.os.Environment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.utils.ProjectUtils

class SettingsViewModel : ViewModel() {

    var syncing: Boolean = false
    var showingSyncDialog: Boolean = false
    var showingFileModDialog: Boolean = false
    var showingVerifySyncDialog: Boolean = false
    var response: ValidationResult<Nothing> = ValidationResult.InProgress
    var max: MutableLiveData<Int> = SyncProgressCounter.max
    var progress: MutableLiveData<Int> = SyncProgressCounter.progress

    suspend fun executeSync(context: Context) {
        response = ValidationResult.InProgress

        val externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        // Validate the directory if we have a directory
        if (externalFilesDir != null)
            response = ProjectUtils.validateFileStructure(externalFilesDir)

        // If the directory is valid import projects
        if (response is ValidationResult.Success<Nothing>) {
            syncing = true
            ProjectUtils.importProjects(TimeLapseDatabase.getInstance(context), externalFilesDir!!)
            syncing = false
        }
    }

}