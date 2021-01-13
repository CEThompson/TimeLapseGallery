package com.vwoom.timelapsegallery.data.entry

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

// TODO: add field to mark that GIF needs to be recreated by workmanager
@Entity(tableName = "project")
data class ProjectEntry(
        var project_name: String?)
{
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    /* For creation with manual ID */
    @Ignore
    constructor(id: Long,
                project_name: String?) : this(project_name) {
        this.id = id
    }
}