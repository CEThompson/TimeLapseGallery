package com.vwoom.timelapsegallery.settings

import android.content.Context
import android.os.Environment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.utils.ImportUtils
import javax.inject.Inject

class SettingsViewModel @Inject constructor() : ViewModel() {

    // Sync state
    var syncing: Boolean = false

    // Dialog state
    var showingSyncDialog: Boolean = false
    var showingFileModDialog: Boolean = false
    var showingVerifySyncDialog: Boolean = false

    // Validation response for syncing
    var response: ValidationResult<List<ImportUtils.ProjectDataBundle>> = ValidationResult.InProgress

    // For showing sync progress
    var projectMax: MutableLiveData<Int> = SyncProgressCounter.projectMax
    var projectProgress: MutableLiveData<Int> = SyncProgressCounter.projectProgress
    var photoMax: MutableLiveData<Int> = SyncProgressCounter.photoMax
    var photoProgress: MutableLiveData<Int> = SyncProgressCounter.photoProgress

    suspend fun executeSync(context: Context) {
        response = ValidationResult.InProgress

        val externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Validate the directory if we have a directory
        if (externalFilesDir != null)
            response = ImportUtils.validateFileStructure(externalFilesDir)

        // If the directory is valid import projects
        if (response is ValidationResult.Success<List<ImportUtils.ProjectDataBundle>>) {
            syncing = true
            val validatedList = (response as ValidationResult.Success<List<ImportUtils.ProjectDataBundle>>).data
            ImportUtils.importProjects(TimeLapseDatabase.getInstance(context), externalFilesDir!!, validatedList)
            syncing = false
            showingSyncDialog = true
        }
    }

}