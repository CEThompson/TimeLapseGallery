package com.vwoom.timelapsegallery.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.TimeLapseDatabase.Companion.getInstance
import com.vwoom.timelapsegallery.data.entry.CoverPhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import java.io.File
import java.util.*

object ProjectUtils {
    private val TAG = ProjectUtils::class.java.simpleName
    fun validateFileStructure(context: Context): String {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: return context.getString(R.string.no_files_error)
        val files = storageDir.listFiles()
                ?: return context.getString(R.string.no_files_in_directory_error, storageDir.absolutePath)
        val projectIds = HashSet<Long>()
        for (child in files) { // Get the filename of the project
            val url = child.absolutePath
            val projectFilename = url.substring(url.lastIndexOf("/") + 1)

            // Skip Temporary Images
            if (projectFilename == FileUtils.TEMP_FILE_SUBDIRECTORY) continue

            // Determine ID of project
            val id = if (projectFilename.lastIndexOf("_") == -1) projectFilename
            else projectFilename.substring(0, projectFilename.lastIndexOf("_"))
            Log.d(TAG, "deriving project id = $id")

            /* Ensure ids are unique */
            val longId = java.lang.Long.valueOf(id)
            if (projectIds.contains(longId)) return context.getString(R.string.duplicate_id_error, projectFilename) else projectIds.add(longId)

            // Determine name of project
            var projectName: String? = null
            if (projectFilename.lastIndexOf("_")>=0)
                projectName = projectFilename.substring(projectFilename.lastIndexOf("_") + 1)
            Log.d(TAG, "deriving project name = $projectName")

            /* Ensure names do not contain reserved characters */
            if (projectName != null
                    && FileUtils.pathContainsReservedCharacter(projectName)) return context.getString(R.string.invalid_character_error, projectFilename, FileUtils.ReservedChars)

            // Get the files within the directory
            val projectFiles = child.listFiles()

            // Check for valid timestamps
            if (projectFiles != null) {
                for (photoFile in projectFiles) {
                    val photoUrl = photoFile.absolutePath
                    val photoFilename = photoUrl.substring(photoUrl.lastIndexOf("/") + 1)
                    try {
                        java.lang.Long.valueOf(photoFilename.replaceFirst("[.][^.]+$".toRegex(), ""))
                    } catch (e: Exception) {
                        return context.getString(R.string.invalid_photo_file_error, photoFilename, projectName)
                    }
                }
            }
        }
        return context.getString(R.string.valid_file_structure)
    }

    /* Helper to scan through folders and import projects */
    suspend fun importProjects(context: Context) {
        Log.d(TAG, "Importing projects")
        val db = getInstance(context)

        // Delete all project references in the database
        db.projectDao().deleteAllProjects()

        // Add all project references from the file structure
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (storageDir != null) {
            val files = storageDir.listFiles()
            if (files != null) { // For each file generate a project
                for (child in files) {
                    // Get the filename of the project
                    val url = child.absolutePath
                    val filename = url.substring(url.lastIndexOf("/") + 1)

                    // Skip Temporary Images
                    if (filename == FileUtils.TEMP_FILE_SUBDIRECTORY) continue

                    // Determine ID of project
                    val id = if (filename.lastIndexOf("_")==-1) filename
                    else filename.substring(0, filename.lastIndexOf("_"))
                    Log.d(TAG, "deriving project id = $id")

                    // Determine name of project
                    var projectName: String? = null
                    if (filename.lastIndexOf("_")>=0)
                        projectName = filename.substring(filename.lastIndexOf("_") + 1)
                    Log.d(TAG, "deriving project name = $projectName")

                    // Get the files within the directory
                    val projectDir = File(storageDir, filename)
                    val projectFiles = projectDir.listFiles()

                    if (projectFiles != null) {
                        // Create the project entry
                        val currentProject = ProjectEntry(
                                java.lang.Long.valueOf(id),
                                projectName, 0)
                        Log.d(TAG, "inserting project = $currentProject")
                        // Insert the project - this updates on conflict
                        db.projectDao().insertProject(currentProject);

                        /* import the photos for the project */
                        importProjectPhotos(storageDir, db, currentProject)
                    }
                }
            }
        }
    }

    /* Finds all photos in the project directory and adds any missing photos to the database */
    private suspend fun importProjectPhotos(externalFilesDir: File, db: TimeLapseDatabase, currentProject: ProjectEntry) {
        Log.d(TAG, "Importing photos for project")
        // Create a list of all photos in the project directory
        val allPhotosInFolder = FileUtils.getPhotoEntriesInProjectDirectory(externalFilesDir, currentProject)
        // Insert the photos from the file structure
        if (allPhotosInFolder != null) {
            // Insert the new entries
            for (photoEntry in allPhotosInFolder) {
                val id = db.photoDao().insertPhoto(photoEntry)
                Log.d(TAG, "inserting photo id $id $photoEntry")
            }
            val lastPhoto = db.photoDao().getLastPhoto(currentProject.id)
            val coverPhoto = CoverPhotoEntry(lastPhoto.project_id, lastPhoto.id)
            Log.d(TAG, "inserting coverphoto $coverPhoto")
            db.coverPhotoDao().insertPhoto(coverPhoto)
        }
    }
}