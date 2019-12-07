package com.vwoom.timelapsegallery.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.vwoom.timelapsegallery.database.AppExecutors;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.database.entry.PhotoEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class FileUtils {

    public static final String ReservedChars = "|\\?*<\":>+[]/'";
    public static final String TEMP_FILE_SUBDIRECTORY = "temporary_images";
    private static final String TAG = FileUtils.class.getSimpleName();

    /* Used to create a photo file in its final location */
    private static File createImageFile(Context context, ProjectEntry currentProject, long timestamp) {
        // Create an image file name from the current timestamp
        String imageFileName = timestamp + ".jpg";
        File projectDir = getProjectFolder(context, currentProject);

        if (!projectDir.exists()) projectDir.mkdirs();

        return new File(projectDir, imageFileName);
    }

    private static File getProjectFolder(Context context, ProjectEntry project){
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String projectPath = getProjectDirectoryPath(project);
        return new File(storageDir, projectPath);
    }

    public static List<PhotoEntry> getPhotosInDirectory(Context context, ProjectEntry project){
        List<PhotoEntry> photos = new ArrayList<>();
        File projectFolder = getProjectFolder(context, project);
        File[] files = projectFolder.listFiles();

        long projectId = project.getId();
        if (files != null) {
            for (File child : files) {
                String url = child.getAbsolutePath();
                String filename = url.substring(url.lastIndexOf("/")+1);
                long timestamp = Long.valueOf(filename.replaceFirst("[.][^.]+$",""));
                PhotoEntry photoEntry = new PhotoEntry(projectId, timestamp);
                photos.add(photoEntry);
            }
        } else return null;

        return photos;
    }

    /* Creates a file in a temporary location */
    public static File createTemporaryImageFile(Context context) throws IOException {
        // Create an image file name
        String imageFileName = "TEMP_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File tempFolder = new File(storageDir, TEMP_FILE_SUBDIRECTORY);
        if (!tempFolder.exists()) tempFolder.mkdirs();

        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                tempFolder      /* directory */
        );
    }

    /* Used to create a photo file from its temporary location */
    public static File createFinalFileFromTemp(
            Context context,
            String tempPath,
            ProjectEntry currentProject,
            long timestamp)
    throws IOException {
        // Create the permanent file for the photo
        File finalFile = createImageFile(context, currentProject, timestamp);
        // Create tempfile from previous path
        File tempFile = new File(tempPath);
        // Copy file to new destination
        copy(tempFile, finalFile);
        // Remove temporary file
        tempFile.delete();

        return finalFile;
    }

    /* Used to copy from one file to another */
    private static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try{
                byte[] buf = new byte[1024];
                int len;
                while ((len=in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    /* Copies a Project from one folder to another */
    public static boolean renameProject(Context context, ProjectEntry source, ProjectEntry destination){
        // Create a file for the source project
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String sourceProjectPath = getProjectDirectoryPath(source);
        File sourceProject = new File(storageDir, sourceProjectPath);

        // Create a file for the destination project
        String destinationProjectPath = getProjectDirectoryPath(destination);
        File destinationProject = new File(storageDir, destinationProjectPath);

        // Rename the folder
        boolean success = sourceProject.renameTo(destinationProject);

        // Update the photo references
        // Return true if rename is successful
        if (success) return true;
        // Return false if rename is not successful
        return false;
    }

    /* Delete temporary directory and files within temporary directory */
    public static void deleteTempFiles(Context context){
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File tempDir = new File(storageDir, TEMP_FILE_SUBDIRECTORY);
        deleteRecursive(tempDir);
        Log.d("deletion check", "delete temp files firing");
    }

    /* Recursive delete used by delete project, delete temp, or delete photo */
    private static void deleteRecursive(File fileOrFileDirectory){
        if (fileOrFileDirectory.isDirectory()){
            for (File child : fileOrFileDirectory.listFiles()){
                deleteRecursive(child);
            }
        }
        Log.d("deletion check", "deleting temporary file named: " + fileOrFileDirectory.getName());
        fileOrFileDirectory.delete();
    }

    /* Delete project directory and files within project directory */
    public static void deleteProject(Context context, ProjectEntry projectEntry){
        String projectDirectoryPath = getProjectDirectoryPath(projectEntry);
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File projectDirectory = new File(storageDir, projectDirectoryPath);
        deleteRecursive(projectDirectory);
    }

    /* Deletes file referred to in photo entry */
    public static void deletePhoto(Context context, ProjectEntry projectEntry, PhotoEntry photoEntry){
        File photoFile = new File(getPhotoUrl(context, projectEntry, photoEntry));
        deleteRecursive(photoFile);
    }

    /* Returns true if a path contains reserved characters */
    public static boolean pathContainsReservedCharacter(String path){
        for (int i = 0; i < ReservedChars.length(); i++){
            char current = ReservedChars.charAt(i);
            if (path.indexOf(current) >= 0) return true;
        }
        return false;
    }

    /* Returns the pattern for a projects path : project path = {project_id}_{project_name} */
    private static String getProjectDirectoryPath(ProjectEntry projectEntry){
        return projectEntry.getId() + "_" + projectEntry.getProject_name();
    }

    public static String getPhotoUrl(Context context, ProjectEntry projectEntry, PhotoEntry photoEntry){
        String imageFileName = getPhotoFileName(photoEntry);
        File projectDir = getProjectFolder(context, projectEntry);
        File photoFile = new File(projectDir, imageFileName);
        return photoFile.getAbsolutePath();
    }

    public static String getPhotoFileName(PhotoEntry entry){
        return entry.getTimestamp() + ".jpg";
    }

    public static String getPhotoFileName(long timestamp){
        return timestamp + ".jpg";
    }
}
