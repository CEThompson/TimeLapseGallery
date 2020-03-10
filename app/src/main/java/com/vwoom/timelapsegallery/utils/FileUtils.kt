package com.vwoom.timelapsegallery.utils

import android.util.Log
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project
import java.io.*
import java.util.*

object FileUtils {
    const val ReservedChars = "|\\?*<\":>+[]/'"
    private val TAG = FileUtils::class.java.simpleName
    const val TEMP_FILE_SUBDIRECTORY = "temporary_images"
    private const val META_FILE_SUBDIRECTORY = "meta"
    const val SCHEDULE_TEXT_FILE = "schedule.txt"
    const val TAGS_DEFINITION_TEXT_FILE = "tags.txt"
    private const val ERROR_TIMESTAMP_TO_PHOTO = "error retrieving photo from timestamp"

    // Creates an image file for a project in the projects folder by project view
    private fun createImageFileForProject(storageDirectory: File, projectEntry: ProjectEntry, timestamp: Long): File {
        val imageFileName = "$timestamp.jpg"
        val projectDir = getProjectFolder(storageDirectory, projectEntry)
        if (!projectDir.exists()) projectDir.mkdirs()
        return File(projectDir, imageFileName)
    }

    private fun getProjectFolder(externalFilesDir: File, projectEntry: ProjectEntry): File {
        val projectPath = getProjectDirectoryPath(projectEntry)
        return File(externalFilesDir, projectPath)
    }

    private fun getProjectFolder(externalFilesDir: File, project: Project): File {
        val projectPath = getProjectDirectoryPath(project)
        return File(externalFilesDir, projectPath)
    }

    fun getMetaDirectoryForProject(externalFilesDir: File, projectEntry: ProjectEntry): File{
        val projectDir = getProjectFolder(externalFilesDir, projectEntry)
        return File(projectDir, META_FILE_SUBDIRECTORY)
    }

    // Creates a list of photo entries in a project folder sorted by timestamp
    fun getPhotoEntriesInProjectDirectory(externalFilesDir: File, projectEntry: ProjectEntry): List<PhotoEntry>? {
        val photos: MutableList<PhotoEntry> = ArrayList()
        val projectFolder = getProjectFolder(externalFilesDir, projectEntry)
        val files = projectFolder.listFiles()
        if (files != null) {
            for (child in files) {
                // Skip directories
                if (!child.isFile) continue

                // Get the timestamp from the url
                val url = child.absolutePath
                val filename = url.substring(url.lastIndexOf("/") + 1)
                val filenameParts = filename.split(".").toTypedArray()
                val timestamp = filenameParts[0].toLong()

                // Create a photo entry for the timestamp
                val photoEntry = PhotoEntry(projectEntry.id, timestamp)
                photos.add(photoEntry)
            }
        } else return null
        // Sort the photo entries by timestamp
        photos.sortBy { it.timestamp }
        return photos
    }

    // Creates a file in the temporary directory
    @JvmStatic
    @Throws(IOException::class)
    fun createTemporaryImageFile(externalFilesDir: File?): File { // Create an image file name
        val imageFileName = "TEMP_"
        val tempFolder = File(externalFilesDir, TEMP_FILE_SUBDIRECTORY)
        if (!tempFolder.exists()) tempFolder.mkdirs()
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",  /* suffix */
                tempFolder /* directory */
        )
    }

    // Creates a file for a project from a temporary file
    @Throws(IOException::class)
    fun createFinalFileFromTemp(
            externalFilesDir: File,
            tempPath: String,
            projectEntry: ProjectEntry,
            timestamp: Long): File {
        // Create the permanent file for the photo
        val finalFile = createImageFileForProject(externalFilesDir, projectEntry, timestamp)
        // Create temporary file from previous path
        val tempFile = File(tempPath)
        // Copy file to new destination
        copy(tempFile, finalFile)
        // Remove temporary file
        tempFile.delete()
        return finalFile
    }

    // Used to copy temp photo file to final photo file
    @Throws(IOException::class)
    private fun copy(src: File, dst: File) {
        val input: InputStream = FileInputStream(src)
        input.use {
            val out: OutputStream = FileOutputStream(dst)
            out.use {
                val buf = ByteArray(1024)
                var len: Int
                while (input.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
            }
        }
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

    // Deletes the temporary directory and files within
    fun deleteTempFiles(externalFilesDir: File?) {
        if (externalFilesDir == null) return
        val tempDir = File(externalFilesDir, TEMP_FILE_SUBDIRECTORY)
        deleteRecursive(tempDir)
    }

    // Recursive delete used by delete project, delete temp, or delete photo
    private fun deleteRecursive(fileOrFileDirectory: File) {
        if (fileOrFileDirectory.isDirectory) {
            val files = fileOrFileDirectory.listFiles()
            if (files!=null)
                for (child in files) {
                    deleteRecursive(child)
                }
        }
        fileOrFileDirectory.delete()
    }

    // Delete project directory and files within project directory
    fun deleteProject(externalFilesDir: File, projectEntry: ProjectEntry) {
        val projectDirectoryPath = getProjectDirectoryPath(projectEntry)
        val projectDirectory = File(externalFilesDir, projectDirectoryPath)
        deleteRecursive(projectDirectory)
    }

    // Deletes file referred to in photo entry by project view
    fun deletePhoto(externalFilesDir: File, projectEntry: ProjectEntry, photoEntry: PhotoEntry) {
        val photoFile = File(getPhotoUrl(externalFilesDir, projectEntry, photoEntry))
        deleteRecursive(photoFile)
    }

    // Returns true if a path contain a reserved character
    fun pathContainsReservedCharacter(path: String): Boolean {
        for (character in ReservedChars){
            if (path.contains(character)) return true
        }
        return false
    }

    // Returns the pattern for a projects path : project path = {project_id}_{project_name}
    // Examples: 1_My Project, 2_Cactus, 3_Flower, etc.
    private fun getProjectDirectoryPath(projectEntry: ProjectEntry): String {
        val name = projectEntry.project_name
        return if (name == null) projectEntry.id.toString()
        else projectEntry.id.toString() + "_" + projectEntry.project_name
    }

    private fun getProjectDirectoryPath(project: Project): String {
        val name = project.project_name
        return if (name == null) project.project_id.toString()
        else project.project_id.toString() + "_" + project.project_name
    }

    fun getPhotoUrl(externalFilesDir: File, projectEntry: ProjectEntry, photoEntry: PhotoEntry): String {
        val imageFileNames = getPhotoFileNames(photoEntry)
        val projectDir = getProjectFolder(externalFilesDir, projectEntry)

        lateinit var photoFile: File
        for (fileName in imageFileNames){
            photoFile = File(projectDir, fileName)
            if (photoFile.exists()) return photoFile.absolutePath
        }
        // TODO handle error case better
        return ERROR_TIMESTAMP_TO_PHOTO
    }

    // TODO Look into improving photo retrieval
    fun getPhotoUrl(externalFilesDir: File, project: Project, photoEntry: PhotoEntry): String {
        val imageFileNames: Array<String> = getPhotoFileNames(photoEntry)
        val projectDir = getProjectFolder(externalFilesDir, project)

        lateinit var photoFile: File
        // Try the timestamp to various file formats, i.e. timestamp.jpeg, timestamp.png, timestamp.jpg
        for (fileName in imageFileNames){
            photoFile = File(projectDir, fileName)
            if (photoFile.exists()) return photoFile.absolutePath
        }
        return ERROR_TIMESTAMP_TO_PHOTO
    }

    fun getCoverPhotoUrl(externalFilesDir: File, project: Project): String {
        val imageFileNames = getPhotoFileNames(project.cover_photo_timestamp)
        val projectDir = getProjectFolder(externalFilesDir, project)

        lateinit var photoFile: File
        for (fileName in imageFileNames){
            photoFile = File(projectDir, fileName)
            if (photoFile.exists()) return photoFile.absolutePath
        }
        return ERROR_TIMESTAMP_TO_PHOTO
    }

    fun getPhotoFileNames(entry: PhotoEntry): Array<String> {
        val timestamp = entry.timestamp.toString()
        return arrayOf("$timestamp.jpg","$timestamp.png","$timestamp.jpeg")
    }

    fun getPhotoFileNames(timestamp: Long): Array<String> {
        return arrayOf("$timestamp.jpg","$timestamp.png","$timestamp.jpeg")
    }

    fun addTagToProject(externalFilesDir: File, project: Project, tags: List<TagEntry>?){
        if (tags == null) return // If no tags no need to write file

        val projectDir = getProjectFolder(externalFilesDir, project)
        val metaDir = File(projectDir, META_FILE_SUBDIRECTORY)

        if (!metaDir.exists()) metaDir.mkdir()

        val tagsFile = File(metaDir, TAGS_DEFINITION_TEXT_FILE)

        // TODO handle writing in try catch block
        // Write the tags to a text file
        val output = FileOutputStream(tagsFile)
        val outputStreamWriter = OutputStreamWriter(output)

        for (tag in tags) {
            outputStreamWriter.write(tag.tag + "\n")
        }
        outputStreamWriter.flush()
        output.fd.sync()
        outputStreamWriter.close()
    }

    fun scheduleProject(externalFilesDir: File, project: Project, projectScheduleEntry: ProjectScheduleEntry){
        val projectDir = getProjectFolder(externalFilesDir, project)
        val metaDir = File(projectDir, META_FILE_SUBDIRECTORY)
        if (!metaDir.exists()) metaDir.mkdir()
        val scheduleFile = File(metaDir, SCHEDULE_TEXT_FILE)

        // Remove the schedule text file if unscheduled
        if (projectScheduleEntry.interval_days == null || projectScheduleEntry.interval_days == 0){
            scheduleFile.delete()
            Log.d(TAG, "deleting schedule file")
        }
        // Otherwise add it
        else {
            Log.d(TAG, "writing schedule file")
            val output = FileOutputStream(scheduleFile)
            val outputStreamWriter = OutputStreamWriter(output)
            outputStreamWriter.write(projectScheduleEntry.interval_days!!.toString())
            outputStreamWriter.flush()
            output.fd.sync()
            outputStreamWriter.close()
        }
    }
}