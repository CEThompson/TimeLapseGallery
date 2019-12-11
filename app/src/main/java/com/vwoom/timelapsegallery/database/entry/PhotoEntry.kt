package com.vwoom.timelapsegallery.database.entry

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "photo")
class PhotoEntry {
    /* Getters *//* Setters */
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    var project_id: Long
    var timestamp: Long

    /* For inserting with auto-generated ID */
    @Ignore
    constructor(project_id: Long, timestamp: Long) {
        this.project_id = project_id
        this.timestamp = timestamp
    }

    constructor(id: Long, project_id: Long, timestamp: Long) {
        this.id = id
        this.project_id = project_id
        this.timestamp = timestamp
    }

}