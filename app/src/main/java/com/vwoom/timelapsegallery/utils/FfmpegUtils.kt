package com.vwoom.timelapsegallery.utils

import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import java.io.File
import java.lang.Exception

object FfmpegUtils {

    fun makeGif(externalFilesDir: File, project: ProjectEntry){
        // TODO implement comment to turn set of photos into gif or mp4

        // get directory for project
        //val projectPhotos: List<PhotoEntry> = ProjectUtils.getPhotoEntriesInProjectDirectory(externalFilesDir, project)


        // TODO use concat demuxer with the list of files
        // list format is: (as a text file)
        //
        // file '/path/to/file1'
        // file '/path/to/file2'
        //
        // etc.

        // then `ffmpeg -f concat -i mylist.txt ... <output>`

        // TODO convert project files to a set of reduced size files

        // TODO make and write text file list of files
        Log.d("TLG.GIF:", "Creating list of text files")
        val listTextFile = FileUtils.createTempListPhotoFiles(externalFilesDir, project)

        // TODO make directory and path for output gif
        Log.d("TLG.GIF:", "Defining output gif path")
        val projectMetaDir = ProjectUtils.getMetaDirectoryForProject(externalFilesDir, project.id)
        val outputGif = "${projectMetaDir.absolutePath}/out.gif"
        Log.d("TLG.GIF:", "Output gif path is: $outputGif")


        //val ffmpegCommand = "-f concat -framerate 9 -i $listTextFile -vf scale=400:-1 $outputGif"
        val ffmpegCommand = "-y -f concat -safe 0 -i $listTextFile -vf scale=400:-1 $outputGif"
        Log.d("TLG.GIF:", "Executing ffmpeg command: $ffmpegCommand")

        val rc = FFmpeg.execute(ffmpegCommand)
        Log.d("TLG.GIF:", "Executed, rc is: $rc")
        val lastCommandOutput = Config.getLastCommandOutput()
        Log.d("TLG.GIF:", "Last command output: $lastCommandOutput")

    }

    fun makeVideo(){

    }
}