package com.vwoom.timelapsegallery.utils;

import android.content.Context;
import android.os.Environment;
import android.text.format.DateUtils;
import android.util.Log;

import com.vwoom.timelapsegallery.R;
import com.vwoom.timelapsegallery.database.AppExecutors;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.database.entry.PhotoEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ProjectUtils {

    private static final String TAG = ProjectUtils.class.getSimpleName();

    /* Takes all scheduled projects and returns a list of projects scheduled for today */
    public static List<ProjectEntry> getProjectsScheduledToday(List<ProjectEntry> projects){
        List<ProjectEntry> projectsForToday = new ArrayList<>();

        /* Go through all scheduled projects and create a list of projects scheduled for today */
        for (ProjectEntry currentProject : projects){

            // Find the next scheduled time
            long nextScheduledPhotoTimestamp = currentProject.getSchedule_next_submission();
            int schedule = currentProject.getSchedule();

            // If that timestamp belongs to today, add it to the list
            // If the timestamp is due up, add the project to the list as well
            if (schedule != 0
                    && (DateUtils.isToday(nextScheduledPhotoTimestamp) || System.currentTimeMillis() > nextScheduledPhotoTimestamp))
                projectsForToday.add(currentProject);
        }

        // Return the list of todays scheduled projects
        return projectsForToday;
    }

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

        /* Ensure no gaps in IDs */
        int size = projectIds.size();
        for (long i = 0; i < size; i++){
            if (!projectIds.contains(i+1)){
                int currentId = (int) i+1;
                return context.getString(R.string.missing_id_error, currentId);
            }
        }

        return context.getString(R.string.valid_file_structure);
    }

    /* Helper to scan through folders and import projects */
    public static void importProjects(Context context){
        Log.d(TAG, "Importing projects");
        TimeLapseDatabase db = TimeLapseDatabase.getInstance(context);

        // Delete all project references in the database
        db.projectDao().deleteAllProjects();
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
                                projectName,
                                projectFiles[0].getAbsolutePath(),
                                TimeUtils.SCHEDULE_NONE,
                                0,
                                0);

                        // Insert the project - this updates on conflict
                        db.projectDao().insertProject(currentProject);

                        /* import the photos for the project */
                        importProjectPhotos(db, currentProject, context);
                    }

                }
            }
        }
    }

    /* Finds all photos in the project directory and adds any missing photos to the database */
    private static void importProjectPhotos(TimeLapseDatabase db, ProjectEntry currentProject, Context context){
        Log.d(TAG, "Importing photos for project");
        // Create a list of all photos in the project directory
        List<PhotoEntry> allPhotosInFolder = FileUtils.getPhotosInDirectory(context, currentProject);

        // Insert the photos from the file structure
        if (allPhotosInFolder!=null) {
            // Insert the new entries
            for (PhotoEntry newEntry : allPhotosInFolder) {
                db.photoDao().insertPhoto(newEntry);
            }
        }

        // Get the resulting list
        List<PhotoEntry> result = db.photoDao().loadAllPhotosByProjectId_NonLiveData(currentProject.getId());

        // Update the project entry with information from first and last photo in set
        PhotoEntry first = result.get(0);
        PhotoEntry last = result.get(result.size()-1);
        currentProject.setThumbnail_url(last.getUrl());
        currentProject.setSchedule_next_submission(last.getTimestamp());
        currentProject.setTimestamp(first.getTimestamp());
        db.projectDao().updateProject(currentProject);
    }
}
