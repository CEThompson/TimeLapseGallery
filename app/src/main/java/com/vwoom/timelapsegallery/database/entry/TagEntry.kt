package com.vwoom.timelapsegallery.database.entry

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "tag")
class TagEntry {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    var tag: String

    /* For inserting with auto-generated ID */
    @Ignore
    constructor(tag: String) {
        this.tag = tag
    }

    constructor(id: Long, tag: String) {
        this.id = id
        this.tag = tag
    }

}