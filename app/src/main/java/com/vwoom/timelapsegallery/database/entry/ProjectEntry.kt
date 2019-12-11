package com.vwoom.timelapsegallery.database.entry

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "project")
data class ProjectEntry(
        var project_name: String?,
        var cover_set_by_user: Int = 0) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    /* For creation with manual ID */
    @Ignore
    constructor(id: Long,
                project_name: String?,
                cover_set_by_user: Int) : this(project_name, cover_set_by_user) {
        this.id = id
    }
}