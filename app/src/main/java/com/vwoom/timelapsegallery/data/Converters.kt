package com.vwoom.timelapsegallery.data

import androidx.room.TypeConverter

class Converters {

    // TODO implement converters?
    @TypeConverter
    fun fromTimestamp(value: Long?): String? {
        return null
    }
}