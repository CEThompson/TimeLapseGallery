package com.vwoom.timelapsegallery.utils;

import android.content.Context;
import android.os.Environment;
import android.text.format.DateUtils;
import android.util.Log;

import com.vwoom.timelapsegallery.database.AppExecutors;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.database.entry.PhotoEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;

import java.io.File;
import java.util.ArrayList;
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


    /* Helper to scan through folders and import projects */
    public static void importProjects(Context context){
        Log.d(TAG, "Importing projects");
        AppExecutors.getInstance().diskIO().execute(()->{
            TimeLapseDatabase db = TimeLapseDatabase.getInstance(context);

            File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (storageDir != null) {
                File[] files = storageDir.listFiles();

                if (files != null) {
                    for (File child : files) {
                        String url = child.getAbsolutePath();
                        Log.d(TAG, "importing url " + url);

                        String filename = url.substring(url.lastIndexOf("/")+1);
                        Log.d(TAG, "stripping to filename = " + filename);

                        if (!filename.equals(FileUtils.TEMP_FILE_SUBDIRECTORY)) {
                            String id = filename.substring(0, filename.lastIndexOf("_"));
                            Log.d(TAG, "stripping to project id = " + id);
                            String projectName = filename.substring(filename.lastIndexOf("_") + 1);
                            Log.d(TAG, "stripping to project name = " + projectName);
                            File projectDir = new File(storageDir, filename);
                            File[] projectFiles = projectDir.listFiles();

                            if (projectFiles != null) {
                                String firstPhotoPath = projectFiles[0].getAbsolutePath();
                                String lastPhotoPath = projectFiles[projectFiles.length-1].getAbsolutePath();

                                String lastPhotoRelPath = lastPhotoPath.substring(lastPhotoPath.lastIndexOf("/")+1);
                                long lastPhotoTimeStamp = Long.valueOf(lastPhotoRelPath.replaceFirst("[.][^.]+$",""));
                                String firstPhotoRelPath = firstPhotoPath.substring(firstPhotoPath.lastIndexOf("/")+1);
                                long firstPhotoTimestamp = Long.valueOf(firstPhotoRelPath.replaceFirst("[.][^.]+$",""));

                                Log.d(TAG, "first photo path = " + firstPhotoPath);
                                Log.d(TAG, "last photo path = " + lastPhotoPath);

                                Log.d(TAG, "inserting project = " + projectName);
                                ProjectEntry currentProject
                                        = new ProjectEntry(
                                        Long.valueOf(id),
                                        projectName,
                                        firstPhotoPath,
                                        TimeUtils.SCHEDULE_NONE,
                                        lastPhotoTimeStamp,
                                        firstPhotoTimestamp);

                                db.projectDao().insertProject(currentProject);

                                /* import the photos for the project */
                                importProjectPhotos(db, currentProject, context);
                            }
                        }
                    }
                }
            }

        });
    }

    /* Finds all photos in the project directory and adds any missing photos to the database */
    public static void importProjectPhotos(TimeLapseDatabase db, ProjectEntry currentProject, Context context){

        Log.d(TAG, "syncing files");
        // Create a list of all photos in the project directory
        List<PhotoEntry> allPhotosInFolder = FileUtils.getPhotosInDirectory(context, currentProject);

        // Create empty list of photos to add
        List<PhotoEntry> photosMissingInDb = new ArrayList<>();

        // Generate a list of photos missing from the database
        if (allPhotosInFolder != null) {
            Log.d(TAG, "checking photos in folder");
            // Loop through all photos in folder
            for (PhotoEntry photo : allPhotosInFolder) {

                long currentTimestamp = photo.getTimestamp();
                Log.d(TAG, "checking timestamp " + currentTimestamp);
                PhotoEntry dbPhoto = db.photoDao().loadPhotoByTimestamp(currentTimestamp, currentProject.getId());

                Log.d(TAG, "dbPhoto is null = " + (dbPhoto == null));
                if (dbPhoto == null) photosMissingInDb.add(photo);
            }
        }

        if (photosMissingInDb.size() == 0) return;

        Log.d(TAG, "photos missing from dabatase is " + photosMissingInDb.toString());
        Log.d(TAG, "adding the missing photos");
        // Add the missing photos to the database
        for (PhotoEntry photo: photosMissingInDb){
            db.photoDao().insertPhoto(photo);
        }
    }

}
