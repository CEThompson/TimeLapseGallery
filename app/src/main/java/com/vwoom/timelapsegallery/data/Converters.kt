package com.vwoom.timelapsegallery.data

import androidx.room.TypeConverter
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.view.Photo
import com.vwoom.timelapsegallery.utils.FileUtils

class Converters {

    // TODO figure out proper converter from timestamp to photo file path
    /*
    @TypeConverter
    fun photoFromPhotoEntry(photoEntry: PhotoEntry): Photo {
        return Photo(
                photoEntry.id,
                photoEntry.timestamp,
                FileUtils.photo
                FileUtils.getPhotoFileName(photoEntry))
    }
    */
}