package com.vwoom.timelapsegallery.utils

import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.utils.ProjectUtils.getMetaDirectoryForProject
import com.vwoom.timelapsegallery.utils.ProjectUtils.getProjectFolder
import timber.log.Timber
import java.io.*

object FileUtils {

    private const val RESERVED_CHARACTERS = "|\\?*<\":>+[]/'"

    // Directory definitions
    private const val TEMP_FILE_SUBDIRECTORY = "temporary_images"
    private const val PROJECTS_FILE_SUBDIRECTORY = "Projects"
    private const val META_FILE_SUBDIRECTORY = "Meta"
    private const val GIF_FILE_SUBDIRECTORY = "Gif"

    // Text files for metadata
    private const val SCHEDULE_DEFINITION_TEXT_FILE = "schedule.txt"
    private const val TAGS_DEFINITION_TEXT_FILE = "tags.txt"
    private const val SENSOR_DEFINITION_TEXT_FILE = "sensorData.txt"
    private const val LIST_PHOTOS_TEXT_FILE = "photosList.txt"

    fun getReservedCharacters() = RESERVED_CHARACTERS

    fun getProjectsSubdirectory(externalFilesDir: File): File {
        return File(externalFilesDir, PROJECTS_FILE_SUBDIRECTORY)
    }

    fun getMetaSubdirectory(externalFilesDir: File): File {
        return File(externalFilesDir, META_FILE_SUBDIRECTORY)
    }

    fun getGifSubdirectory(externalFilesDir: File): File {
        return File(externalFilesDir, GIF_FILE_SUBDIRECTORY)
    }

    fun getTempFolder(externalFilesTestDir: File): File {
        return File(externalFilesTestDir, TEMP_FILE_SUBDIRECTORY)
    }


    fun getTagsFile(projectMetaDir: File): File {
        return File(projectMetaDir, TAGS_DEFINITION_TEXT_FILE)
    }

    fun getScheduleFile(projectMetaDir: File): File{
        return File(projectMetaDir, SCHEDULE_DEFINITION_TEXT_FILE)
    }

    fun getSensorFile(projectMetaDir: File): File {
        return File(projectMetaDir, SENSOR_DEFINITION_TEXT_FILE)
    }

    // Creates an empty image file for a project in the projects folder to be copied to
    private fun createImageFileForProject(externalFilesDir: File, projectEntry: ProjectEntry, timestamp: Long): File {
        val imageFileName = "$timestamp.jpg"
        val projectDir = getProjectFolder(externalFilesDir, projectEntry)
        if (!projectDir.exists()) projectDir.mkdirs()
        return File(projectDir, imageFileName)
    }

    // Creates an empty image file in the temporary directory to be written to by the camera
    @JvmStatic
    @Throws(IOException::class)
    fun createImageFileInTemporaryFolder(externalFilesDir: File?): File {
        val imageFilePrefix = "TEMP_"
        val suffixExtension = ".jpg"
        val temporaryFolder = File(externalFilesDir, TEMP_FILE_SUBDIRECTORY)
        if (!temporaryFolder.exists()) temporaryFolder.mkdirs()
        return File.createTempFile(
                imageFilePrefix,  // prefix
                suffixExtension,  // suffix
                temporaryFolder // directory
        )
    }

    // Copies from temporary photo file to the final photo for a project
    @Throws(IOException::class)
    fun createProjectPhotoFileFromTemporaryPhoto(
            externalFilesDir: File,
            tempPath: String,
            projectEntry: ProjectEntry,
            timestamp: Long): File {
        // 1. First create the permanent file for the photo
        val finalFile = createImageFileForProject(externalFilesDir, projectEntry, timestamp)
        // 2. Get reference to the temp file
        val tempFile = File(tempPath)
        // 3. Copy from temp to the final destination
        copyFile(tempFile, finalFile)
        // 4. Clean up / remove the temporary file
        tempFile.delete()
        return finalFile
    }

    // Used internal to class to copy from one file to another (should be from the temp photo to final photo)
    @Throws(IOException::class)
    private fun copyFile(src: File, destination: File) {
        val input: InputStream = FileInputStream(src)
        input.use {
            val out: OutputStream = FileOutputStream(destination)
            out.use {
                val buf = ByteArray(1024)
                var len: Int
                while (input.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
            }
        }
    }

    // Deletes the temporary directory and its contents
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

    // Supports .jpg, .png, .jpeg creation from timestamp
    fun getPhotoFileExtensions(timestamp: Long): Array<String> {
        return arrayOf("$timestamp.jpg", "$timestamp.png", "$timestamp.jpeg")
    }

    // This function Writes the tags for a project in a text file in the meta directory for the project
    // Ex. a project of ID 1 will have a path of ...meta/1/tags.txt
    // In general this is used to decouple tag data from the room data (for use in loading data on and off devices)
    fun writeProjectTagsFile(
            externalFilesDir: File,
            projectId: Long,
            // TODO (1.4) : look into preserving tag order so that lineage can be preserved for plants (mother x father designation). Currently tags are sorted alphabetically somewhere
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
            Timber.e("error writing tag to text file: ${exception.message}")
        }
    }

    // Writes the schedule for a project in the projects meta directory similarly to the tag writing function
    // This text file should contain a simple integer indicating the interval between scheduled photos in days
    // (For use in loading data on and off devices)
    fun writeProjectScheduleFile(
            externalFilesDir: File,
            projectId: Long,
            projectScheduleEntry: ProjectScheduleEntry) {
        val metaDir = getMetaDirectoryForProject(externalFilesDir, projectId)
        val scheduleFile = File(metaDir, SCHEDULE_DEFINITION_TEXT_FILE)

        try {
            val output = FileOutputStream(scheduleFile)
            val outputStreamWriter = OutputStreamWriter(output)
            outputStreamWriter.write(projectScheduleEntry.interval_days.toString())
            outputStreamWriter.flush()
            output.fd.sync()
            outputStreamWriter.close()
        } catch (exception: IOException) {
            Timber.e("error writing schedule to text file: ${exception.message}")
        }
    }

    // Creates a temporary text file which contains a list of the photo urls for a project
    // This text file is used to control ffmpeg in order to create a GIF from the set of photos
    // The format required by ffmpeg is as follows:
    // file '/path/to/file1'
    // file '/path/to/file2'
    // etc.
    fun createTempListPhotoFiles(
            externalFilesDir: File,
            projectEntry: ProjectEntry): File? {
        // 1. First clean the temp files
        deleteTempFiles(externalFilesDir)

        // 2. Get the list of photos to convert
        val listOfPhotos = ProjectUtils.getPhotoEntriesInProjectDirectory(externalFilesDir, projectEntry)

        // 3. Create the temporary folder and define the text file
        val tempFolder = File(externalFilesDir, TEMP_FILE_SUBDIRECTORY)
        tempFolder.mkdir()
        val listFiles = File(tempFolder, LIST_PHOTOS_TEXT_FILE)

        // Write the file paths to the text file
        try {
            val output = FileOutputStream(listFiles)
            val outputStreamWriter = OutputStreamWriter(output)
            // For each photo in the list write its path
            for (photo in listOfPhotos) {
                val photoUrlString = ProjectUtils.getProjectPhotoUrl(externalFilesDir, projectEntry, photo.timestamp)
                val fileDefString = "file '$photoUrlString'\n" // file '/path/to/file1'
                outputStreamWriter.write(fileDefString)
            }
            outputStreamWriter.flush()
            output.fd.sync()
            outputStreamWriter.close()
        }
        catch (exception: IOException) {
            return null
        }
        return listFiles
    }

    // Writes sensor data (if available) for each photo to a text file
    // Should be a list of strings in the following format:
    // timestamp(long) light(lx) temperature(celsius) pressure(mbar) humidity(%)
    fun writeSensorData(externalFilesDir: File, photoEntry: PhotoEntry, projectId: Long) {
        val metaDir = getMetaDirectoryForProject(externalFilesDir, projectId)
        val sensorDataFile = File(metaDir, SENSOR_DEFINITION_TEXT_FILE)

        val entryText = "${photoEntry.timestamp} ${photoEntry.light} ${photoEntry.temp} ${photoEntry.pressure} ${photoEntry.humidity}\n"

        try {
            sensorDataFile.appendText(entryText)
        } catch (exception: IOException){
            Timber.e("error writing sensor data to text file: ${exception.message}")
        }
    }

}