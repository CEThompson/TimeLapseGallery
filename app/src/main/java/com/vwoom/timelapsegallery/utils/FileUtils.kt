package com.vwoom.timelapsegallery.utils

import android.util.Log
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.utils.ProjectUtils.getMetaDirectoryForProject
import com.vwoom.timelapsegallery.utils.ProjectUtils.getProjectFolder
import java.io.*

const val RESERVED_CHARACTERS = "|\\?*<\":>+[]/'"

object FileUtils {
    private val TAG = FileUtils::class.java.simpleName

    // Directory definitions
    const val TEMP_FILE_SUBDIRECTORY = "temporary_images"
    const val META_FILE_SUBDIRECTORY = "Meta"
    const val GIF_FILE_SUBDIRECTORY = "Gif"

    // Text files for metadata
    const val SCHEDULE_TEXT_FILE = "schedule.txt"
    const val TAGS_DEFINITION_TEXT_FILE = "tags.txt"
    private const val LIST_PHOTOS_TEXT_FILE = "photos_list.txt"

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

    // Recursive deletion of a file or directory
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

    // Returns true if a path contains a reserved character
    fun pathContainsReservedCharacter(path: String): Boolean {
        for (character in RESERVED_CHARACTERS) {
            if (path.contains(character)) return true
        }
        return false
    }

    // Support .jpg, .png, .jpeg creation from timestamp
    fun getPhotoFileExtensions(timestamp: Long): Array<String> {
        return arrayOf("$timestamp.jpg", "$timestamp.png", "$timestamp.jpeg")
    }

    // Write the tags for a project in a text file in the projects meta directory
    // For use in loading data on and off devices
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

    // Writes the schedule for a project in the projects meta directory
    // For use in loading data on and off devices
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

    // TODO: write test for temp list
    // Creates a temporary text file list of the photo urls for a project
    // In the format:
    // file '/path/to/file1'
    // file '/path/to/file2'
    // etc.
    fun createTempListPhotoFiles(
            externalFilesDir: File,
            project: ProjectEntry): File? {

        // First clean the temp files
        //Log.d("TLG.GIF:", "Clearing temp directory")
        deleteTempFiles(externalFilesDir)

        // Get the list of photos to convert
        //Log.d("TLG.GIF:", "Getting list of photos to convert")
        val photosToConvert = ProjectUtils.getPhotoEntriesInProjectDirectory(externalFilesDir, project)

        // Create the temporary folder and define the text file
        val tempFolder = File(externalFilesDir, TEMP_FILE_SUBDIRECTORY)
        tempFolder.mkdir()
        val listFiles = File(tempFolder, LIST_PHOTOS_TEXT_FILE)

        // Writ the file paths to the text file
        //Log.d("TLG.GIF:", "Writing list of photos to convert")
        try {
            val output = FileOutputStream(listFiles)
            val outputStreamWriter = OutputStreamWriter(output)
            // For each photo write the path
            for (photo in photosToConvert) {
                val photoUrlString = ProjectUtils.getProjectPhotoUrl(externalFilesDir, project, photo.timestamp)
                val fileDefString = "file '$photoUrlString'\n"
                Log.d("TLG.GIF:", "Writing: $fileDefString")
                outputStreamWriter.write(fileDefString)
            }
            // Clean up
            outputStreamWriter.flush()
            output.fd.sync()
            outputStreamWriter.close()
        }
        // If exception caught return null
        catch (exception: IOException) {
            //Log.d("TLG.GIF:", "Caught exception trying to write list of files: ${exception.message}")
            return null
        }
        return listFiles
    }

}