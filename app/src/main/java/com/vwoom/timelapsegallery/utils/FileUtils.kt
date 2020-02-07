package com.vwoom.timelapsegallery.utils

import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project
import java.io.*
import java.util.*

object FileUtils {
    const val ReservedChars = "|\\?*<\":>+[]/'"
    const val TEMP_FILE_SUBDIRECTORY = "temporary_images"
    const val META_FILE_SUBDIRECTORY = "meta"
    const val TAGS_DEFINITION_TEXT_FILE = "tags.txt"
    private val TAG = FileUtils::class.java.simpleName

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
            tempPath: String?,
            projectEntry: ProjectEntry,
            timestamp: Long): File {
        // Create the permanent file for the photo
        val finalFile = createImageFileForProject(externalFilesDir, projectEntry, timestamp)
        // Create tempfile from previous path
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

    // Copies a Project from one folder to another: For use in renaming a project
    fun renameProject(externalFilesDir: File, sourceProject: ProjectEntry, destinationProject: ProjectEntry): Boolean { // Create a file for the source project
        val sourceProjectPath = getProjectDirectoryPath(sourceProject)
        val sourceProject = File(externalFilesDir, sourceProjectPath)
        // Create a file for the destination project
        val destinationProjectPath = getProjectDirectoryPath(destinationProject)
        val destinationProject = File(externalFilesDir, destinationProjectPath)
        // Rename the folder
        val success = sourceProject.renameTo(destinationProject)

        // Return true if rename is successful
        return success
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
            for (child in fileOrFileDirectory.listFiles()) {
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
        for (i in 0 until ReservedChars.length) {
            val current = ReservedChars[i]
            if (path.indexOf(current) >= 0) return true
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
        return "error retrieving image file from timestamp"
    }

    fun getPhotoUrl(externalFilesDir: File, project: Project, photoEntry: PhotoEntry): String {
        val imageFileNames: Array<String> = getPhotoFileNames(photoEntry)
        val projectDir = getProjectFolder(externalFilesDir, project)

        lateinit var photoFile: File
        for (fileName in imageFileNames){
            photoFile = File(projectDir, fileName)
            if (photoFile.exists()) return photoFile.absolutePath
        }
        // TODO handle error case better
        return "error retrieving image file from timestamp"
    }

    fun getCoverPhotoUrl(externalFilesDir: File, project: Project): String {
        val imageFileNames = getPhotoFileNames(project.cover_photo_timestamp)
        val projectDir = getProjectFolder(externalFilesDir, project)

        lateinit var photoFile: File
        for (fileName in imageFileNames){
            photoFile = File(projectDir, fileName)
            if (photoFile.exists()) return photoFile.absolutePath
        }
        // TODO handle error case better
        return "error retrieving image file from timestamp"
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

        val tagsFile = File(metaDir, "tags.txt")

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
}