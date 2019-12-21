package com.vwoom.timelapsegallery.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.vwoom.timelapsegallery.R;
import com.vwoom.timelapsegallery.data.TimeLapseDatabase;
import com.vwoom.timelapsegallery.data.entry.PhotoEntry;
import com.vwoom.timelapsegallery.data.entry.ProjectEntry;

import java.io.File;
import java.util.HashSet;
import java.util.List;

public class ProjectUtils {

    private static final String TAG = ProjectUtils.class.getSimpleName();

    public static String validateFileStructure(Context context){
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir==null) return context.getString(R.string.no_files_error);

        File[] files = storageDir.listFiles();

        if (files == null) return context.getString(R.string.no_files_in_directory_error, storageDir.getAbsolutePath());

        HashSet<Long> projectIds = new HashSet<>();

        for (File child : files) {
            // Get the filename of the project
            String url = child.getAbsolutePath();
            String projectFilename = url.substring(url.lastIndexOf("/")+1);

            // Skip Temporary Images
            if (projectFilename.equals(FileUtils.TEMP_FILE_SUBDIRECTORY))continue;

            // Determine ID of project
            String id = projectFilename.substring(0, projectFilename.lastIndexOf("_"));
            Log.d(TAG, "deriving project id = " + id);

            /* Ensure ids are unique */
            Long longId = Long.valueOf(id);
            if (projectIds.contains(longId))
                return context.getString(R.string.duplicate_id_error, projectFilename);
            else projectIds.add(longId);

            // Determine name of project
            String projectName = projectFilename.substring(projectFilename.lastIndexOf("_") + 1);
            Log.d(TAG, "deriving project name = " + projectName);

            /* Ensure names do not contain reserved characters */
            if (FileUtils.pathContainsReservedCharacter(projectName))
                return context.getString(R.string.invalid_character_error, projectFilename, FileUtils.ReservedChars);

            // Get the files within the directory
            File[] projectFiles = child.listFiles();

            // Check for valid timestamps
            if (projectFiles != null){
                for (File photoFile : projectFiles) {
                    String photoUrl = photoFile.getAbsolutePath();
                    String photoFilename = photoUrl.substring(photoUrl.lastIndexOf("/")+1);
                    try {
                        Long.valueOf(photoFilename.replaceFirst("[.][^.]+$", ""));
                    } catch (Exception e){
                        return context.getString(R.string.invalid_photo_file_error, photoFilename, projectName);
                    }
                }
            }
        }

        return context.getString(R.string.valid_file_structure);
    }

    /* Helper to scan through folders and import projects */
    public static void importProjects(Context context){
        Log.d(TAG, "Importing projects");
        TimeLapseDatabase db = TimeLapseDatabase.Companion.getInstance(context);

        // Delete all project references in the database
        //TODO delete all projects db.projectDao().deleteAllProjects();
        db.photoDao().deleteAllPhotos();

        // Add all project references from the file structure
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null) {
            File[] files = storageDir.listFiles();

            if (files != null) {
                // For each file generate a project
                for (File child : files) {
                    // Get the filename of the project
                    String url = child.getAbsolutePath();
                    String filename = url.substring(url.lastIndexOf("/")+1);

                    // Skip Temporary Images
                    if (filename.equals(FileUtils.TEMP_FILE_SUBDIRECTORY))continue;

                    // Determine ID of project
                    String id = filename.substring(0, filename.lastIndexOf("_"));
                    Log.d(TAG, "deriving project id = " + id);

                    // Determine name of project
                    String projectName = filename.substring(filename.lastIndexOf("_") + 1);
                    Log.d(TAG, "deriving project name = " + projectName);

                    // Get the files within the directory
                    File projectDir = new File(storageDir, filename);
                    File[] projectFiles = projectDir.listFiles();

                    if (projectFiles != null) {
                        Log.d(TAG, "inserting project = " + projectName);

                        // Create the project entry
                        ProjectEntry currentProject
                                = new ProjectEntry(
                                Long.valueOf(id),
                                projectName,0);

                        // Insert the project - this updates on conflict
                        // TODO insert project :: db.projectDao().insertProject(currentProject);

                        /* import the photos for the project */
                        importProjectPhotos(storageDir, db, currentProject);
                    }

                }
            }
        }
    }

    /* Finds all photos in the project directory and adds any missing photos to the database */
    private static void importProjectPhotos(File externalFilesDir, TimeLapseDatabase db, ProjectEntry currentProject){
        Log.d(TAG, "Importing photos for project");
        // Create a list of all photos in the project directory
        List<PhotoEntry> allPhotosInFolder = FileUtils.getPhotoEntriesInProjectDirectory(externalFilesDir, currentProject);

        // Insert the photos from the file structure
        if (allPhotosInFolder!=null) {
            // Insert the new entries
            for (PhotoEntry newEntry : allPhotosInFolder) {
                db.photoDao().insertPhoto(newEntry);
            }
        }
    }
}
