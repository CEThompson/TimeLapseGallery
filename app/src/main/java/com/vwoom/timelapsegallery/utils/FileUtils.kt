package com.vwoom.timelapsegallery.utils

import android.util.Log
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project
import java.io.*
import java.util.*

const val RESERVED_CHARACTERS = "|\\?*<\":>+[]/'"

object FileUtils {
    private val TAG = FileUtils::class.java.simpleName
    const val TEMP_FILE_SUBDIRECTORY = "temporary_images"
    const val META_FILE_SUBDIRECTORY = "Meta"
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

    fun getProjectFolder(externalFilesDir: File, projectEntry: ProjectEntry): File {
        val projectPath = getProjectDirectoryPath(projectEntry)
        return File(externalFilesDir, projectPath)
    }

    fun getMetaDirectoryForProject(externalFilesDir: File, projectId: Long): File{
        val metaDir = File(externalFilesDir, META_FILE_SUBDIRECTORY)
        val projectSubfolder = File(metaDir, projectId.toString())
        projectSubfolder.mkdirs()
        return projectSubfolder
    }

    // Creates a list of photo entries in a project folder sorted by timestamp
    fun getPhotoEntriesInProjectDirectory(externalFilesDir: File,
                                          projectEntry: ProjectEntry): List<PhotoEntry>? {
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
        val metaProjectDirectory = getMetaDirectoryForProject(externalFilesDir, projectEntry.id)
        // Delete the project photo files
        deleteRecursive(projectDirectory)
        // Delete the metadata for the project
        deleteRecursive(metaProjectDirectory)
    }

    // Deletes file referred to in photo entry by project view
    fun deletePhoto(externalFilesDir: File, projectEntry: ProjectEntry, photoEntry: PhotoEntry) {
        val photoUrl = getPhotoUrl(externalFilesDir, projectEntry, photoEntry.timestamp)
        if (photoUrl == ERROR_TIMESTAMP_TO_PHOTO) return // photo file does not exist already
        val photoFile = File(photoUrl)
        deleteRecursive(photoFile)
    }

    // Returns true if a path contain a reserved character
    fun pathContainsReservedCharacter(path: String): Boolean {
        for (character in RESERVED_CHARACTERS){
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

    fun getPhotoUrl(externalFilesDir: File, projectEntry: ProjectEntry, timestamp: Long): String {
        val imageFileNames = getPhotoFileNames(timestamp)
        val projectDir = getProjectFolder(externalFilesDir, projectEntry)

        lateinit var photoFile: File
        // Try the timestamp to various file formats, i.e. timestamp.jpeg, timestamp.png, timestamp.jpg
        for (fileName in imageFileNames){
            photoFile = File(projectDir, fileName)
            if (photoFile.exists()) return photoFile.absolutePath
        }
        return ERROR_TIMESTAMP_TO_PHOTO
    }

    fun getPhotoFileNames(timestamp: Long): Array<String> {
        return arrayOf("$timestamp.jpg","$timestamp.png","$timestamp.jpeg")
    }

    fun addTagToProject(externalFilesDir: File, projectId: Long, tags: List<TagEntry>){
        val metaDir = getMetaDirectoryForProject(externalFilesDir, projectId)
        val tagsFile = File(metaDir, TAGS_DEFINITION_TEXT_FILE)

        // Write the tags to a text file
        // TODO: determine how to handle output stream writer exceptions
        try {
            val output = FileOutputStream(tagsFile)
            val outputStreamWriter = OutputStreamWriter(output)
            for (tag in tags) {
                outputStreamWriter.write(tag.text + "\n")
            }
            outputStreamWriter.flush()
            output.fd.sync()
            outputStreamWriter.close()
        } catch (exception: IOException) {
            Log.e(TAG, "error writing tag to text file: ${exception.message}")
        }
    }

    fun scheduleProject(externalFilesDir: File, projectId: Long, projectScheduleEntry: ProjectScheduleEntry){
        val metaDir = getMetaDirectoryForProject(externalFilesDir, projectId)
        val scheduleFile = File(metaDir, SCHEDULE_TEXT_FILE)

        Log.d(TAG, "writing schedule file")
        // TODO: determine how to handle output stream writer exceptions
        try {
            val output = FileOutputStream(scheduleFile)
            val outputStreamWriter = OutputStreamWriter(output)
            outputStreamWriter.write(projectScheduleEntry.interval_days.toString())
            outputStreamWriter.flush()
            output.fd.sync()
            outputStreamWriter.close()
        } catch (exception: IOException) {
            Log.e(TAG, "error writing schedule to text file: ${exception.message}")
        }
    }
}