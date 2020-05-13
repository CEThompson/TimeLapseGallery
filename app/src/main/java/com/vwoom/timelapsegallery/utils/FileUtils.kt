package com.vwoom.timelapsegallery.utils

import android.util.Log
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.utils.ProjectUtils.getMetaDirectoryForProject
import com.vwoom.timelapsegallery.utils.ProjectUtils.getProjectFolder
import com.vwoom.timelapsegallery.weather.ForecastResponse
import java.io.*

const val RESERVED_CHARACTERS = "|\\?*<\":>+[]/'"

object FileUtils {
    private val TAG = FileUtils::class.java.simpleName

    // Directory definitions
    const val TEMP_FILE_SUBDIRECTORY = "temporary_images"
    const val META_FILE_SUBDIRECTORY = "Meta"

    // Text files for metadata
    const val SCHEDULE_TEXT_FILE = "schedule.txt"
    const val TAGS_DEFINITION_TEXT_FILE = "tags.txt"

    // Creates an image file for a project in the projects folder by project view
    private fun createImageFileForProject(storageDirectory: File, projectEntry: ProjectEntry, timestamp: Long): File {
        val imageFileName = "$timestamp.jpg"
        val projectDir = getProjectFolder(storageDirectory, projectEntry)
        if (!projectDir.exists()) projectDir.mkdirs()
        return File(projectDir, imageFileName)
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
        copyFile(tempFile, finalFile)
        // Remove temporary file
        tempFile.delete()

        return finalFile
    }

    // Used to copy temp photo file to final photo file
    @Throws(IOException::class)
    private fun copyFile(src: File, dst: File) {
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

    // Deletes the temporary directory and files within
    fun deleteTempFiles(externalFilesDir: File?) {
        if (externalFilesDir == null) return
        val tempDir = File(externalFilesDir, TEMP_FILE_SUBDIRECTORY)
        deleteRecursive(tempDir)
    }

    // Recursive delete used by delete project, delete temp, or delete photo
    fun deleteRecursive(fileOrFileDirectory: File) {
        if (fileOrFileDirectory.isDirectory) {
            val files = fileOrFileDirectory.listFiles()
            if (files != null)
                for (child in files) {
                    deleteRecursive(child)
                }
        }
        fileOrFileDirectory.delete()
    }

    // Returns true if a path contain a reserved character
    fun pathContainsReservedCharacter(path: String): Boolean {
        for (character in RESERVED_CHARACTERS) {
            if (path.contains(character)) return true
        }
        return false
    }

    fun getPhotoFileExtensions(timestamp: Long): Array<String> {
        return arrayOf("$timestamp.jpg", "$timestamp.png", "$timestamp.jpeg")
    }

    // TODO: (update 1.2) determine how to handle output stream writer exceptions for writing project tags and project schedule
    fun writeProjectTagsFile(
            externalFilesDir: File,
            projectId: Long,
            tags: List<TagEntry>) {
        val metaDir = getMetaDirectoryForProject(externalFilesDir, projectId)
        val tagsFile = File(metaDir, TAGS_DEFINITION_TEXT_FILE)

        // Write the tags to a text file
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

    fun writeProjectScheduleFile(
            externalFilesDir: File,
            projectId: Long,
            projectScheduleEntry: ProjectScheduleEntry) {
        val metaDir = getMetaDirectoryForProject(externalFilesDir, projectId)
        val scheduleFile = File(metaDir, SCHEDULE_TEXT_FILE)
        Log.d(TAG, "writing schedule file")
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