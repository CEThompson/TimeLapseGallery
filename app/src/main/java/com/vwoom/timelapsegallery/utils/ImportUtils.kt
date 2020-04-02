package com.vwoom.timelapsegallery.utils

import android.util.Log
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.entry.*
import com.vwoom.timelapsegallery.settings.SyncProgressCounter
import com.vwoom.timelapsegallery.settings.ValidationResult
import java.io.File
import java.io.FileInputStream
import java.util.*

object ImportUtils {
    private val TAG = ImportUtils::class.java.simpleName

    // Used to bundle a project with the number of photos in it
    // Passed to the project import utility
    // List of projects and photo import used to provide user feedback on status of import
    data class ProjectDataBundle(val projectEntry: ProjectEntry, val photoCount: Int)

    // Ensures no errors in the file structure, returns a list of structurally sound projects with the numbers of photos
    // for the project
    fun validateFileStructure(externalFilesDir: File): ValidationResult<List<ProjectDataBundle>> {
        // Final output list
        val resultList = arrayListOf<ProjectDataBundle>()

        val files = externalFilesDir.listFiles()
        if (files == null || files.isEmpty())
            return ValidationResult.Error.NoFilesError(null, externalFilesDir.absolutePath)

        val projectIds = HashSet<Long>()

        // Get project directories
        for (child in files) {
            // Get the filename of the project
            val url = child.absolutePath
            val projectFilename = url.substring(url.lastIndexOf(File.separatorChar) + 1)

            // Skip Temporary Images
            if (projectFilename == FileUtils.TEMP_FILE_SUBDIRECTORY
                    || projectFilename == FileUtils.META_FILE_SUBDIRECTORY) continue

            // Determine ID of project
            val idString: String =
                    if (projectFilename.lastIndexOf("_") == -1) projectFilename
                    else projectFilename.substring(0, projectFilename.lastIndexOf("_"))

            /* Ensure ids are unique */
            var currentId: Long?
            try {
                currentId = idString.toLong()
                if (projectIds.contains(currentId))
                    return ValidationResult.Error.DuplicateIdError(null, projectFilename)
                projectIds.add(currentId)
            } catch (e: NumberFormatException) {
                return ValidationResult.Error.InvalidFolder(e, projectFilename)
            }

            // Determine name of project
            var projectName: String? = null
            val indexOfIdNameSeparator = projectFilename.lastIndexOf("_")
            if (indexOfIdNameSeparator > 0)
                projectName = projectFilename.substring(indexOfIdNameSeparator + 1)

            /* Ensure names do not contain reserved characters */
            if (projectName != null && FileUtils.pathContainsReservedCharacter(projectName))
                return ValidationResult.Error.InvalidCharacterError(null, projectName)

            // Get the files within the directory
            val projectFiles = child.listFiles()

            // Check for valid timestamps
            var photoCounter = 0
            if (projectFiles != null) {
                for (file in projectFiles) {
                    if (file.isDirectory) continue  // skips the meta subfolder
                    val photoUrl = file.absolutePath
                    val photoFilename = photoUrl.substring(photoUrl.lastIndexOf(File.separatorChar) + 1)
                    try {
                        java.lang.Long.valueOf(photoFilename.replaceFirst("[.][^.]+$".toRegex(), ""))
                        photoCounter++
                    } catch (e: Exception) {
                        return ValidationResult.Error.InvalidPhotoFileError(e, photoUrl, projectName)
                    }
                }
            }
            val currentProject = ProjectEntry(currentId, projectName)
            val currentDataBundle = ProjectDataBundle(currentProject, photoCounter)
            resultList.add(currentDataBundle)
        }
        Log.d(TAG, "return validation result, list of size ${resultList.size}")
        return ValidationResult.Success(resultList.toList())
    }

    /* Helper to scan through folders and import projects */
    suspend fun importProjects(db: TimeLapseDatabase,
                               externalFilesDir: File,
                               projectBundles: List<ProjectDataBundle>,
                               testing: Boolean = false) {

        // Delete projects and tags in the database: should clear all tables by cascade
        db.projectDao().deleteAllProjects()
        db.tagDao().deleteAllTags()

        if (!testing) {
            SyncProgressCounter.initProjectCount(projectBundles.size)
            Log.d("ProgressCheck", "setting max ${SyncProgressCounter.projectMax}")
        }

        for (projectBundle in projectBundles) {
            if (!testing) {
                SyncProgressCounter.incrementProject()
                Log.d("ProgressCheck", "incrementing progress ${SyncProgressCounter.projectProgress}")
            }

            // Get the files within the directory
            val projectDir = ProjectUtils.getProjectFolder(externalFilesDir, projectBundle.projectEntry)
            val projectFiles = projectDir.listFiles()

            if (projectFiles != null) {
                // Create the project entry
                val currentProject = projectBundle.projectEntry
                Log.d(TAG, "inserting project = $currentProject")
                // Insert the project - this updates on conflict
                db.projectDao().insertProject(currentProject)

                // Recover photos
                if (!testing) {
                    SyncProgressCounter.initPhotoCount(projectBundle.photoCount)
                }
                importProjectPhotos(externalFilesDir, db, currentProject, testing)

                // Recover tags
                importProjectMetaData(externalFilesDir, db, currentProject)
            }
        }
    }

    /* Finds all photos in the project directory and adds any missing photos to the database */
    private suspend fun importProjectPhotos(externalFilesDir: File,
                                            db: TimeLapseDatabase,
                                            currentProject: ProjectEntry,
                                            testing: Boolean = false) {
        //Log.d(TAG, "Importing photos for project")
        // Create a list of all photos in the project directory
        val allPhotosInFolder = ProjectUtils.getPhotoEntriesInProjectDirectory(externalFilesDir, currentProject)
        // Insert the photos from the file structure
        if (allPhotosInFolder != null) {
            // Insert the new entries
            for (photoEntry in allPhotosInFolder) {
                db.photoDao().insertPhoto(photoEntry)
                //Log.d(TAG, "inserting photo id $id $photoEntry")
                if (!testing) {
                    SyncProgressCounter.incrementPhoto()
                }
            }
            val lastPhoto = db.photoDao().getLastPhoto(currentProject.id)
            val coverPhoto = CoverPhotoEntry(lastPhoto.project_id, lastPhoto.id)
            //Log.d(TAG, "inserting cover photo $coverPhoto")
            db.coverPhotoDao().insertPhoto(coverPhoto)
        }
    }

    private suspend fun importProjectMetaData(externalFilesDir: File, db: TimeLapseDatabase, currentProject: ProjectEntry) {
        val metaDir = ProjectUtils.getMetaDirectoryForProject(externalFilesDir, currentProject.id)
        val tagsFile = File(metaDir, FileUtils.TAGS_DEFINITION_TEXT_FILE)
        val scheduleFile = File(metaDir, FileUtils.SCHEDULE_TEXT_FILE)

        // Import tags
        if (tagsFile.exists()) {
            //Log.d(TAG, "importing project tags for $currentProject")
            val inputAsString = FileInputStream(tagsFile).bufferedReader().use { it.readText() }
            val tags: List<String> = inputAsString.split('\n')

            //Log.d(TAG, "handling $tags")
            // Convert text to tag entries and enter into database
            for (text in tags) {
                if (text.isEmpty()) continue
                Log.d(TAG, "handling $text")
                // Get the tag
                var tagEntry: TagEntry? = db.tagDao().getTagByText(text)
                // If it does not exist insert into tag table
                if (tagEntry == null) {
                    tagEntry = TagEntry(text)
                    val tagId = db.tagDao().insertTag(tagEntry)
                    tagEntry.id = tagId
                    Log.d(TAG, "inserted $tagEntry")
                }

                val projectTagEntry = ProjectTagEntry(currentProject.id, tagEntry.id)
                // Insert tag and project tag into db
                db.projectTagDao().insertProjectTag(projectTagEntry)
                Log.d(TAG, "inserted $projectTagEntry")
            }
        }

        // Import schedule
        if (scheduleFile.exists()) {
            val inputAsString = FileInputStream(scheduleFile).bufferedReader().use { it.readText() }
            try {
                val projectScheduleEntry = ProjectScheduleEntry(currentProject.id, inputAsString.toInt())
                db.projectScheduleDao().insertProjectSchedule(projectScheduleEntry)
            } catch (e: Exception) {
                Log.e(TAG, "error importing schedule for $currentProject")
            }
        }
    }
}