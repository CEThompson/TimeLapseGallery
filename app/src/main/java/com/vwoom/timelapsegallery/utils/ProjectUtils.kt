package com.vwoom.timelapsegallery.utils

import android.os.Environment
import android.util.Log
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.entry.*
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.settings.ValidationResult
import java.io.File
import java.io.FileInputStream
import java.util.*

object ProjectUtils {
    private val TAG = ProjectUtils::class.java.simpleName

    fun validateFileStructure(externalFilesDir: File): ValidationResult<Nothing> {
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
            if (projectFilename == FileUtils.TEMP_FILE_SUBDIRECTORY) continue

            // Determine ID of project
            val id = if (projectFilename.lastIndexOf("_") == -1) projectFilename
            else projectFilename.substring(0, projectFilename.lastIndexOf("_"))

            /* Ensure ids are unique */
            val currentId = java.lang.Long.valueOf(id)
            if (projectIds.contains(currentId))
                return ValidationResult.Error.DuplicateIdError(null, projectFilename)
            projectIds.add(currentId)

            // Determine name of project
            var projectName: String? = null
            if (projectFilename.lastIndexOf("_") >= 0)
                projectName = projectFilename.substring(projectFilename.lastIndexOf("_") + 1)
            //Log.d(TAG, "deriving project name = $projectName")

            /* Ensure names do not contain reserved characters */
            if (projectName != null && FileUtils.pathContainsReservedCharacter(projectName))
                return ValidationResult.Error.InvalidCharacterError(null, projectName)

            // Get the files within the directory
            val projectFiles = child.listFiles()

            // Check for valid timestamps
            if (projectFiles != null) {
                for (file in projectFiles) {
                    if (file.isDirectory) continue  // skips the meta subfolder
                    val photoUrl = file.absolutePath
                    val photoFilename = photoUrl.substring(photoUrl.lastIndexOf(File.separatorChar) + 1)
                    try {
                        java.lang.Long.valueOf(photoFilename.replaceFirst("[.][^.]+$".toRegex(), ""))
                    } catch (e: Exception) {
                        return ValidationResult.Error.InvalidPhotoFileError(e, photoUrl, projectName)
                    }
                }
            }
        }
        return ValidationResult.Success()
    }

    /* Helper to scan through folders and import projects */
    // Todo: Implement test for project import
    suspend fun importProjects(db: TimeLapseDatabase, externalFilesDir: File) {

        // Delete projects and tags in the database: should clear all tables by cascade
        db.projectDao().deleteAllProjects()
        db.tagDao().deleteAllTags()

        // Add all project references from the file structure
        val files = externalFilesDir.listFiles()
        for (child in files!!) {
            // Get the filename of the project
            val url = child.absolutePath
            val filename = url.substring(url.lastIndexOf(File.separatorChar) + 1)

            // Skip Temporary Images
            if (filename == FileUtils.TEMP_FILE_SUBDIRECTORY) continue

            // Determine ID of project
            val id = if (filename.lastIndexOf("_") == -1) filename
            else filename.substring(0, filename.lastIndexOf("_"))
            Log.d(TAG, "deriving project id = $id")

            // Determine name of project
            var projectName: String? = null
            if (filename.lastIndexOf("_") >= 0)
                projectName = filename.substring(filename.lastIndexOf("_") + 1)
            Log.d(TAG, "deriving project name = $projectName")

            // Get the files within the directory
            val projectDir = File(externalFilesDir, filename)
            val projectFiles = projectDir.listFiles()

            if (projectFiles != null) {
                // Create the project entry
                val currentProject = ProjectEntry(
                        java.lang.Long.valueOf(id),
                        projectName)
                Log.d(TAG, "inserting project = $currentProject")
                // Insert the project - this updates on conflict
                db.projectDao().insertProject(currentProject);

                // Recover photos
                importProjectPhotos(externalFilesDir, db, currentProject)

                // Recover tags
                importProjectMetaData(externalFilesDir, db, currentProject)
            }
        }
    }

    /* Finds all photos in the project directory and adds any missing photos to the database */
    private suspend fun importProjectPhotos(externalFilesDir: File, db: TimeLapseDatabase, currentProject: ProjectEntry) {
        //Log.d(TAG, "Importing photos for project")
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

    private suspend fun importProjectMetaData(externalFilesDir: File, db: TimeLapseDatabase, currentProject: ProjectEntry) {
        val metaDir = FileUtils.getMetaDirectoryForProject(externalFilesDir, currentProject)
        val tagsFile = File(metaDir, FileUtils.TAGS_DEFINITION_TEXT_FILE)
        val scheduleFile = File(metaDir, FileUtils.SCHEDULE_TEXT_FILE)

        // Import tags
        if (tagsFile.exists()) {
            Log.d(TAG, "importing project tags for $currentProject")

            val inputAsString = FileInputStream(tagsFile).bufferedReader().use { it.readText() }
            val tags: List<String> = inputAsString.split('\n')

            Log.d(TAG, "handling $tags")
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
            // TODO test this
            val inputAsString = FileInputStream(scheduleFile).bufferedReader().use { it.readText() }
            try {
                val projectScheduleEntry = ProjectScheduleEntry(currentProject.id, inputAsString.toInt())
                db.projectScheduleDao().insertProjectSchedule(projectScheduleEntry)
            } catch (e: Exception) {
                Log.e(TAG, "error importing schedule for $currentProject")
            }
        }
    }

    fun isProjectDueToday(project: Project): Boolean {
        if (project.interval_days == 0) return false
        val daysSinceLastPhoto = TimeUtils.getDaysSinceTimeStamp(project.cover_photo_timestamp, System.currentTimeMillis())
        val daysUntilDue = project.interval_days - daysSinceLastPhoto
        return daysUntilDue <= 0
    }

    fun isProjectDueTomorrow(project: Project): Boolean {
        if (project.interval_days == 0) return false
        val daysSinceLastPhoto = TimeUtils.getDaysSinceTimeStamp(project.cover_photo_timestamp, System.currentTimeMillis())
        val daysUntilDue = project.interval_days - daysSinceLastPhoto
        return daysUntilDue == 1.toLong()
    }

}

