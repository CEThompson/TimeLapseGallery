package com.vwoom.timelapsegallery.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.view.Project
import java.io.*
import java.util.*

object FileUtils {
    const val ReservedChars = "|\\?*<\":>+[]/'"
    const val TEMP_FILE_SUBDIRECTORY = "temporary_images"
    private val TAG = FileUtils::class.java.simpleName
    /* Used to create a photo file in its final location */
    private fun createImageFileForProject(storageDirectory: File, currentProject: ProjectEntry, timestamp: Long): File { // Create an image file name from the current timestamp
        val imageFileName = "$timestamp.jpg"
        val projectDir = getProjectFolder(storageDirectory, currentProject)
        if (!projectDir.exists()) projectDir.mkdirs()
        return File(projectDir, imageFileName)
    }

    private fun createImageFileForProject(storageDirectory: File, project: Project, timestamp: Long): File { // Create an image file name from the current timestamp
        val imageFileName = "$timestamp.jpg"
        val projectDir = getProjectFolder(storageDirectory, project)
        if (!projectDir.exists()) projectDir.mkdirs()
        return File(projectDir, imageFileName)
    }

    private fun getProjectFolder(externalFilesDir: File, project: ProjectEntry): File { //File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        val projectPath = getProjectDirectoryPath(project)
        return File(externalFilesDir, projectPath)
    }

    private fun getProjectFolder(externalFilesDir: File, project: Project): File { //File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        val projectPath = getProjectDirectoryPath(project)
        return File(externalFilesDir, projectPath)
    }

    /* This method scans through a project folder and creates a list for each photo in it,
    skipping interior folders and non-image files */
    fun getPhotoEntriesInProjectDirectory(externalFilesDir: File, project: ProjectEntry): List<PhotoEntry>? {
        val photos: MutableList<PhotoEntry> = ArrayList()
        val projectFolder = getProjectFolder(externalFilesDir, project)
        val files = projectFolder.listFiles()
        if (files != null) {
            for (child in files) {
                if (!child.isFile) continue  // If the child is a directory skip it
                val url = child.absolutePath
                val filename = url.substring(url.lastIndexOf("/") + 1)
                // Split the filename at the extension
                val filenameParts = filename.split(".").toTypedArray()
                val extension = filenameParts[1]

                if (extension == "txt") continue  // If the child is a text file skip it
                // This regex matches and replaces (removes) the file extension
// long timestamp = Long.valueOf(filename.replaceFirst("[.][^.]+$",""));
                val timestamp = java.lang.Long.valueOf(filenameParts[0])
                val photoEntry = PhotoEntry(project.id, timestamp)
                photos.add(photoEntry)
            }
        } else return null
        // Sort the photo entries by timestamp
        photos.sortBy { it.timestamp }
        return photos
    }

    /* Creates a file in a temporary location */
    @JvmStatic
    @Throws(IOException::class)
    fun createTemporaryImageFile(externalFilesDir: File?): File { // Create an image file name
        val imageFileName = "TEMP_"
        //File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        val tempFolder = File(externalFilesDir, TEMP_FILE_SUBDIRECTORY)
        if (!tempFolder.exists()) tempFolder.mkdirs()
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",  /* suffix */
                tempFolder /* directory */
        )
    }

    /* Used to create a photo file from its temporary location */
    @Throws(IOException::class)
    fun createFinalFileFromTemp(
            externalFilesDir: File,
            tempPath: String?,
            currentProject: ProjectEntry,
            timestamp: Long): File { // Create the permanent file for the photo
        val finalFile = createImageFileForProject(externalFilesDir, currentProject, timestamp)
        // Create tempfile from previous path
        val tempFile = File(tempPath)
        // Copy file to new destination
        copy(tempFile, finalFile)
        // Remove temporary file
        tempFile.delete()
        return finalFile
    }

    @Throws(IOException::class)
    fun createFinalFileFromTemp(
            externalFilesDir: File,
            tempPath: String?,
            project: Project,
            timestamp: Long): File { // Create the permanent file for the photo
        val finalFile = createImageFileForProject(externalFilesDir, project, timestamp)
        // Create tempfile from previous path
        val tempFile = File(tempPath)
        // Copy file to new destination
        copy(tempFile, finalFile)
        // Remove temporary file
        tempFile.delete()
        return finalFile
    }

    /* Used to copy from one file to another */
    @Throws(IOException::class)
    private fun copy(src: File, dst: File) {
        val `in`: InputStream = FileInputStream(src)
        try {
            val out: OutputStream = FileOutputStream(dst)
            try {
                val buf = ByteArray(1024)
                var len: Int
                while (`in`.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
            } finally {
                out.close()
            }
        } finally {
            `in`.close()
        }
    }

    /* Copies a Project from one folder to another */
    fun renameProject(context: Context, source: ProjectEntry, destination: ProjectEntry): Boolean { // Create a file for the source project
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val sourceProjectPath = getProjectDirectoryPath(source)
        val sourceProject = File(storageDir, sourceProjectPath)
        // Create a file for the destination project
        val destinationProjectPath = getProjectDirectoryPath(destination)
        val destinationProject = File(storageDir, destinationProjectPath)
        // Rename the folder
        val success = sourceProject.renameTo(destinationProject)
        // Update the photo references
// Return true if rename is successful
        return if (success) true else false
        // Return false if rename is not successful
    }

    /* Delete temporary directory and files within temporary directory */
    fun deleteTempFiles(context: Context) {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val tempDir = File(storageDir, TEMP_FILE_SUBDIRECTORY)
        deleteRecursive(tempDir)
        Log.d("deletion check", "delete temp files firing")
    }

    /* Recursive delete used by delete project, delete temp, or delete photo */
    private fun deleteRecursive(fileOrFileDirectory: File) {
        if (fileOrFileDirectory.isDirectory) {
            for (child in fileOrFileDirectory.listFiles()) {
                deleteRecursive(child)
            }
        }
        Log.d("deletion check", "deleting temporary file named: " + fileOrFileDirectory.name)
        fileOrFileDirectory.delete()
    }

    /* Delete project directory and files within project directory */
    fun deleteProject(externalFilesDir: File, projectEntry: ProjectEntry) {
        val projectDirectoryPath = getProjectDirectoryPath(projectEntry)
        val projectDirectory = File(externalFilesDir, projectDirectoryPath)
        deleteRecursive(projectDirectory)
    }

    /* Deletes file referred to in photo entry */
    fun deletePhoto(externalFilesDir: File, projectEntry: ProjectEntry, photoEntry: PhotoEntry) {
        val photoFile = File(getPhotoUrl(externalFilesDir, projectEntry, photoEntry))
        deleteRecursive(photoFile)
    }

    fun deletePhoto(externalFilesDir: File, project: Project, photoEntry: PhotoEntry) {
        val photoFile = File(getPhotoUrl(externalFilesDir, project, photoEntry))
        deleteRecursive(photoFile)
    }

    /* Returns true if a path contains reserved characters */
    fun pathContainsReservedCharacter(path: String): Boolean {
        for (i in 0 until ReservedChars.length) {
            val current = ReservedChars[i]
            if (path.indexOf(current) >= 0) return true
        }
        return false
    }

    /* Returns the pattern for a projects path : project path = {project_id}_{project_name}
    * example: 1_My Project
    * */
    private fun getProjectDirectoryPath(projectEntry: ProjectEntry): String {
        val name = projectEntry.project_name
        return if (name == null) projectEntry.id.toString() else projectEntry.id.toString() + "_" + projectEntry.project_name
    }

    private fun getProjectDirectoryPath(project: Project): String {
        return if (project.project_name == null) project.project_id.toString() else project.project_id.toString() + "_" + project.project_name
    }

    fun getPhotoUrl(externalFilesDir: File, projectEntry: ProjectEntry, photoEntry: PhotoEntry): String {
        val imageFileName = getPhotoFileName(photoEntry)
        val projectDir = getProjectFolder(externalFilesDir, projectEntry)
        val photoFile = File(projectDir, imageFileName)
        return photoFile.absolutePath
    }

    fun getPhotoUrl(externalFilesDir: File, project: Project, photoEntry: PhotoEntry): String {
        val imageFileName = getPhotoFileName(photoEntry)
        val projectDir = getProjectFolder(externalFilesDir, project)
        val photoFile = File(projectDir, imageFileName)
        return photoFile.absolutePath
    }

    fun getCoverPhotoUrl(externalFilesDir: File, project: Project): String {
        val imageFileName = getPhotoFileName(project.cover_photo_timestamp)
        val projectDir = getProjectFolder(externalFilesDir, project)
        val photoFile = File(projectDir, imageFileName)
        return photoFile.absolutePath
    }

    fun getPhotoFileName(entry: PhotoEntry): String {
        return entry.timestamp.toString() + ".jpg"
    }

    fun getPhotoFileName(timestamp: Long): String {
        return "$timestamp.jpg"
    }
}