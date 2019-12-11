package com.vwoom.timelapsegallery.database.entry

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "photo")
data class PhotoEntry(var project_id: Long,
                      var timestamp: Long) {
    /* Getters *//* Setters */
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    /* For inserting with auto-generated ID */
    @Ignore
    constructor(id: Long, project_id: Long, timestamp: Long) : this(project_id, timestamp) {
        this.id = id
    }

}