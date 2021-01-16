package com.vwoom.timelapsegallery.utils

import com.vwoom.timelapsegallery.camera2.SensorData
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.view.ProjectView
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.collections.HashMap

object ProjectUtils {

    fun getProjectFolder(externalFilesDir: File, projectEntry: ProjectEntry): File {
        val projectsSubDir = FileUtils.getProjectsSubdirectory(externalFilesDir)
        val projectPath = getProjectDirectoryPath(projectEntry)
        return File(projectsSubDir, projectPath)
    }

    fun getProjectEntryFromProjectView(projectView: ProjectView): ProjectEntry = ProjectEntry(projectView.project_id, projectView.project_name)

    fun getMetaDirectoryForProject(externalFilesDir: File, projectId: Long): File {
        val metaDir = File(externalFilesDir, FileUtils.META_FILE_SUBDIRECTORY)
        val projectSubfolder = File(metaDir, projectId.toString())
        if (!projectSubfolder.exists()) projectSubfolder.mkdirs()
        return projectSubfolder
    }

    // todo retrieve stored sensor data here
    // Creates a list of photo entries in a project folder sorted by timestamp
    fun getPhotoEntriesInProjectDirectory(externalFilesDir: File,
                                          projectEntry: ProjectEntry): List<PhotoEntry> {
        val photos: MutableList<PhotoEntry> = ArrayList()
        val projectFolder = getProjectFolder(externalFilesDir, projectEntry)

        // For use in restoring sensor data from text file
        val map: Map<Long, SensorData> = getMapFromSensorData(externalFilesDir, projectEntry)
        Timber.d("for project ${projectEntry.id} map is $map")

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

                val sensorData: SensorData? = map[timestamp]
                // Create a photo entry for the timestamp
                val photoEntry = PhotoEntry(projectEntry.id, timestamp,
                        light = sensorData?.light,
                        pressure = sensorData?.pressure,
                        temp = sensorData?.temp,
                        humidity = sensorData?.humidity)
                photos.add(photoEntry)
            }
        }
        // Sort the photo entries by timestamp
        photos.sortBy { it.timestamp }
        return photos
    }

    // TODO figure out how to handle units
    private fun getMapFromSensorData(externalFilesDir: File, projectEntry: ProjectEntry): Map<Long, SensorData> {
        val metaDir = getMetaDirectoryForProject(externalFilesDir, projectEntry.id)
        val sensorTextFile = File(metaDir, FileUtils.SENSOR_DEFINITION_TEXT_FILE)

        val map: MutableMap<Long, SensorData> = HashMap()

        if (!sensorTextFile.exists()) return map.toMap()

        Timber.d("at project ${projectEntry.id}")

        try {
            val inputStream: InputStream = sensorTextFile.inputStream()

            inputStream.bufferedReader().forEachLine {
                // Note: follows the following format
                // timestamp light temp pressure humidity
                val line: String = it
                if (line.isNotEmpty()) {
                    val sub: List<String> = line.split(" ")
                    val timestamp: Long = sub[0].toLong()
                    val light = sub[1]
                    val temp = sub[2]
                    val pressure = sub[3]
                    val humidity = sub[4]

                    Timber.d("reading line $line")
                    val sensorData = SensorData(light = light, temp = temp, pressure = pressure, humidity = humidity)
                    map[timestamp] = sensorData
                }
            }
        } catch (e: Exception){
            Timber.e("exception getting map from sensor data: ${e.localizedMessage}")
        }
        return map.toMap()
    }

    // Copies a Project from one folder to another: For use in renaming a project
    fun renameProject(externalFilesDir: File, sourceProjectEntry: ProjectEntry, destinationProjectEntry: ProjectEntry): Boolean { // Create a file for the source project
        val sourceProject: File = getProjectFolder(externalFilesDir, sourceProjectEntry)
        val destinationProject: File = getProjectFolder(externalFilesDir, destinationProjectEntry)
        // Rename the folder: returns true if successful and false if not
        return sourceProject.renameTo(destinationProject)
    }

    // Delete project directory and files within project directory
    fun deleteProject(externalFilesDir: File, projectEntry: ProjectEntry) {
        val projectDirectory = getProjectFolder(externalFilesDir, projectEntry)
        val metaProjectDirectory = getMetaDirectoryForProject(externalFilesDir, projectEntry.id)
        // Delete the project photo files
        FileUtils.deleteRecursive(projectDirectory)
        // Delete the metadata for the project
        FileUtils.deleteRecursive(metaProjectDirectory)
    }

    // Deletes file referred to in photo entry by project view
    fun deleteProjectPhoto(externalFilesDir: File, projectEntry: ProjectEntry, photoEntry: PhotoEntry) {
        // photo file does not exist already return
        val photoUrl = getProjectPhotoUrl(externalFilesDir, projectEntry, photoEntry.timestamp)
                ?: return
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

