package com.vwoom.timelapsegallery.database.entry

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "project")
class ProjectEntry {
    /* Getters *//* Setters */
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    var project_name: String?
    var cover_set_by_user = 0

    /* For inserting with auto-generated ID */
    @Ignore
    constructor(project_name: String?,
                cover_set_by_user: Int) {
        this.project_name = project_name
        this.cover_set_by_user = cover_set_by_user
    }

    constructor(id: Long,
                project_name: String?,
                cover_set_by_user: Int) {
        this.id = id
        this.project_name = project_name
        this.cover_set_by_user = cover_set_by_user
    }

}