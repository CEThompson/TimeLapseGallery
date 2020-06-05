package com.vwoom.timelapsegallery.utils

import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.view.ProjectView
import java.io.File
import java.util.*

object ProjectUtils {

    fun getProjectFolder(externalFilesDir: File, projectEntry: ProjectEntry): File {
        val projectPath = getProjectDirectoryPath(projectEntry)
        return File(externalFilesDir, projectPath)
    }

    fun getProjectEntryFromProjectView(projectView: ProjectView): ProjectEntry = ProjectEntry(projectView.project_id, projectView.project_name)

    fun getMetaDirectoryForProject(externalFilesDir: File, projectId: Long): File {
        val metaDir = File(externalFilesDir, FileUtils.META_FILE_SUBDIRECTORY)
        val projectSubfolder = File(metaDir, projectId.toString())
        projectSubfolder.mkdirs()
        return projectSubfolder
    }

    // Creates a list of photo entries in a project folder sorted by timestamp
    fun getPhotoEntriesInProjectDirectory(externalFilesDir: File,
                                          projectEntry: ProjectEntry): List<PhotoEntry> {
        val photos: MutableList<PhotoEntry> = ArrayList()
        val projectFolder = getProjectFolder(externalFilesDir, projectEntry)
        val files = projectFolder.listFiles()
        if (files != null) {
            for (child in files) {
                // Skip directories
                if (!child.isFile) continue

                // Get the timestamp from the url
                val url = child.absolutePath
                val filename = url.substring(url.lastIndexOf(File.separatorChar) + 1)
                val filenameParts = filename.split(".").toTypedArray()
                val timestamp = filenameParts[0].toLong()

                // Create a photo entry for the timestamp
                val photoEntry = PhotoEntry(projectEntry.id, timestamp)
                photos.add(photoEntry)
            }
        }
        // Sort the photo entries by timestamp
        photos.sortBy { it.timestamp }
        return photos
    }


    // Copies a Project from one folder to another: For use in renaming a project
    fun renameProject(externalFilesDir: File, sourceProjectEntry: ProjectEntry, destinationProjectEntry: ProjectEntry): Boolean { // Create a file for the source project
        val sourceProjectPath = getProjectDirectoryPath(sourceProjectEntry)
        val sourceProject = File(externalFilesDir, sourceProjectPath)
        // Create a file for the destination project
        val destinationProjectPath = getProjectDirectoryPath(destinationProjectEntry)
        val destinationProject = File(externalFilesDir, destinationProjectPath)

        // Rename the folder: returns true if successful and false if not
        return sourceProject.renameTo(destinationProject)
    }

    // Delete project directory and files within project directory
    fun deleteProject(externalFilesDir: File, projectEntry: ProjectEntry) {
        val projectDirectoryPath = getProjectDirectoryPath(projectEntry)
        val projectDirectory = File(externalFilesDir, projectDirectoryPath)
        val metaProjectDirectory = getMetaDirectoryForProject(externalFilesDir, projectEntry.id)
        // Delete the project photo files
        FileUtils.deleteRecursive(projectDirectory)
        // Delete the metadata for the project
        FileUtils.deleteRecursive(metaProjectDirectory)
    }

    // Deletes file referred to in photo entry by project view
    fun deleteProjectPhoto(externalFilesDir: File, projectEntry: ProjectEntry, photoEntry: PhotoEntry) {
        // photo file does not exist already return
        val photoUrl = getProjectPhotoUrl(externalFilesDir, projectEntry, photoEntry.timestamp) ?: return
        val photoFile = File(photoUrl)
        FileUtils.deleteRecursive(photoFile)
    }

    // Returns the pattern for a projects path : project path = {project_id}_{project_name}
    // Examples: 1_My Project, 2_Cactus, 3_Flower, etc.
    private fun getProjectDirectoryPath(projectEntry: ProjectEntry): String {
        val name = projectEntry.project_name
        return if (name.isNullOrEmpty()) projectEntry.id.toString()
        else projectEntry.id.toString() + "_" + projectEntry.project_name
    }

    fun getProjectPhotoUrl(externalFilesDir: File, projectEntry: ProjectEntry, timestamp: Long): String? {
        val imageFileNames = FileUtils.getPhotoFileExtensions(timestamp)
        val projectDir = getProjectFolder(externalFilesDir, projectEntry)

        lateinit var photoFile: File
        // Try the timestamp to various file formats, i.e. timestamp.jpeg, timestamp.png, timestamp.jpg
        for (fileName in imageFileNames) {
            photoFile = File(projectDir, fileName)
            if (photoFile.exists()) return photoFile.absolutePath
        }
        return null
    }

    fun isProjectDueToday(projectView: ProjectView): Boolean {
        if (projectView.interval_days == 0) return false
        val daysSinceLastPhoto = TimeUtils.getDaysSinceTimeStamp(projectView.cover_photo_timestamp, System.currentTimeMillis())
        val daysUntilDue = projectView.interval_days - daysSinceLastPhoto
        return daysUntilDue <= 0
    }

    fun isProjectDueTomorrow(projectView: ProjectView): Boolean {
        if (projectView.interval_days == 0) return false
        val daysSinceLastPhoto = TimeUtils.getDaysSinceTimeStamp(projectView.cover_photo_timestamp, System.currentTimeMillis())
        val daysUntilDue = projectView.interval_days - daysSinceLastPhoto
        return daysUntilDue == 1.toLong()
    }
}

