package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.data.dao.CoverPhotoDao
import com.vwoom.timelapsegallery.data.entry.CoverPhotoEntry
import com.vwoom.timelapsegallery.data.entry.PhotoEntry

class CoverPhotoRepository private constructor(val coverPhotoDao: CoverPhotoDao) {

    suspend fun setCoverPhoto(entry: PhotoEntry) {
        coverPhotoDao.insertPhoto(CoverPhotoEntry(entry.project_id, entry.id))
    }

    companion object {
        @Volatile private var instance: CoverPhotoRepository? = null

        fun getInstance(coverPhotoDao: CoverPhotoDao) =
                instance ?: synchronized(this) {
                    instance ?: CoverPhotoRepository(coverPhotoDao).also { instance = it }
                }
    }

}