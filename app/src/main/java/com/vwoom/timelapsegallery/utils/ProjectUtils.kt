package com.vwoom.timelapsegallery.utils

import com.vwoom.timelapsegallery.camera2.SensorData
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.view.ProjectView
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.collections.HashMap

object ProjectUtils {

    // Returns the folder for a project containing its photos
    fun getProjectFolder(externalFilesDir: File, projectEntry: ProjectEntry): File {
        val projectsSubDir = FileUtils.getProjectsSubdirectory(externalFilesDir)
        val projectPath = getProjectDirectoryPath(projectEntry)
        return File(projectsSubDir, projectPath)
    }

    // Internal helper returning the pattern for a projects path
    // In the following format: {project_id}_{project_name}
    // If there is no project name then the format is simply: {project_id}
    // Examples: 1_My Project, 2_Cactus, 3_Flower, 4, 143, 154_my plant, etc.
    private fun getProjectDirectoryPath(projectEntry: ProjectEntry): String {
        val name = projectEntry.project_name
        return if (name.isNullOrEmpty()) projectEntry.id.toString()
        else projectEntry.id.toString() + "_" + projectEntry.project_name
    }

    // Converts a project view to a project entry
    fun getProjectEntryFromProjectView(projectView: ProjectView): ProjectEntry =
            ProjectEntry(projectView.project_id, projectView.project_name)

    // Returns the meta directory for a project
    // Example: if a project is Projects/1_my project
    // This will return the folder: Meta/1
    fun getMetaDirectoryForProject(externalFilesDir: File, projectId: Long): File {
        val metaDir = FileUtils.getMetaSubdirectory(externalFilesDir)
        val projectSubfolder = File(metaDir, projectId.toString())
        if (!projectSubfolder.exists()) projectSubfolder.mkdirs()
        return projectSubfolder
    }

    // Returns the tags file for a project
    fun getProjectTagsFile(externalFilesDir: File, id: Long): File {
        val metaDir = getMetaDirectoryForProject(externalFilesDir, id)
        return FileUtils.getTagsFile(metaDir)
    }

    // Returns the a projects schedule file
    fun getProjectScheduleFile(externalFilesDir: File, id: Long): File {
        val metaDir = getMetaDirectoryForProject(externalFilesDir, id)
        return FileUtils.getScheduleFile(metaDir)
    }

    // Returns the sensor data for a project
    fun getSensorDataFile(externalFilesDir: File, id: Long): File {
        val metaDir = getMetaDirectoryForProject(externalFilesDir, id)
        return FileUtils.getSensorFile(metaDir)
    }

    // Returns a list of photo entries in a project folder sorted by timestamp
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

    // Returns a map of timestamps in long format (representing photos)
    // to the accompanying sensor data in the projects sensor data text file
    private fun getMapFromSensorData(externalFilesDir: File, projectEntry: ProjectEntry): Map<Long, SensorData> {
        val metaDir = getMetaDirectoryForProject(externalFilesDir, projectEntry.id)
        val sensorTextFile = FileUtils.getSensorFile(metaDir)

        val map: MutableMap<Long, SensorData> = HashMap()

        if (!sensorTextFile.exists()) return map.toMap() // If no sensor file return an empty map

        // Otherwise create the map
        try {
            val inputStream: InputStream = sensorTextFile.inputStream()

            inputStream.bufferedReader().forEachLine {
                // Note: follows the following format
                // timestamp light temp pressure humidity
                val line: String = it
                // TODO: (deferred) re-evaluate units for sensor data
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

    // Copies a project from one folder to another
    // Used for renaming a project
    fun renameProject(externalFilesDir: File, sourceProjectEntry: ProjectEntry, destinationProjectEntry: ProjectEntry): Boolean { // Create a file for the source project
        val sourceProject: File = getProjectFolder(externalFilesDir, sourceProjectEntry)
        val destinationProject: File = getProjectFolder(externalFilesDir, destinationProjectEntry)
        // Rename the folder: returns true if successful and false if not
        return sourceProject.renameTo(destinationProject)
    }

    // Delete project directory and files within project directory
    // Note: this deletes the Projects/{Id}_{Name} folder and
    // the accompanying Meta/{Id} folder
    // However this leaves any created Gifs from the project behind (In the Gif folder at Gif/{Id}.gif)
    // TODO: consider splitting this into two functions, retiring a successful time-lapse project and deleting a failed project (which would remove the accompanying GIF?)
    fun deleteProject(externalFilesDir: File, projectEntry: ProjectEntry) {
        val projectDirectory = getProjectFolder(externalFilesDir, projectEntry)
        val metaProjectDirectory = getMetaDirectoryForProject(externalFilesDir, projectEntry.id)
        // Delete the project photo files
        FileUtils.deleteRecursive(projectDirectory)
        // Delete the metadata for the project
        FileUtils.deleteRecursive(metaProjectDirectory)
    }

    // Deletes file referred to in photo entry
    fun deleteProjectPhoto(externalFilesDir: File, projectEntry: ProjectEntry, photoEntry: PhotoEntry) {
        // Get the url of the photo
        val photoUrl = getProjectPhotoUrl(externalFilesDir, projectEntry, photoEntry.timestamp)

        // If the photo file does not exist already return
        photoUrl?: return

        // Delete the photo
        val photoFile = File(photoUrl)
        FileUtils.deleteRecursive(photoFile)
    }

    // Returns the Url for a project's photo
    fun getProjectPhotoUrl(externalFilesDir: File, projectEntry: ProjectEntry, timestamp: Long): String? {
        // get supported filenames from the timestamp
        // in the format: {timestamp}.jpg, {timestamp}.jpeg, {timestamp}.png
        val imageFileNames: Array<String> = FileUtils.getPhotoFileExtensions(timestamp)
        val projectDir = getProjectFolder(externalFilesDir, projectEntry)

        lateinit var photoFile: File
        for (fileName in imageFileNames) {
            photoFile = File(projectDir, fileName)
            if (photoFile.exists()) return photoFile.absolutePath // If the file with extension is there return its path
        }
        return null
    }

    // Returns whether or not a project is due today
    fun isProjectDueToday(projectView: ProjectView): Boolean {
        if (projectView.interval_days == 0) return false    // If the project isn't scheduled it cannot be due
        val daysSinceLastPhoto = TimeUtils.getDaysSinceTimeStamp(projectView.cover_photo_timestamp, System.currentTimeMillis())
        val daysUntilDue = projectView.interval_days - daysSinceLastPhoto
        return daysUntilDue <= 0    // Otherwise project is due if time has elapsed past its interval
    }

    // Returns whether or not a project is due tomorrow
    fun isProjectDueTomorrow(projectView: ProjectView): Boolean {
        if (projectView.interval_days == 0) return false
        val daysSinceLastPhoto = TimeUtils.getDaysSinceTimeStamp(projectView.cover_photo_timestamp, System.currentTimeMillis())
        val daysUntilDue = projectView.interval_days - daysSinceLastPhoto
        return daysUntilDue == 1.toLong()
    }

}

