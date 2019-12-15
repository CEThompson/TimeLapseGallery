package com.vwoom.timelapsegallery.data.entry

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "tag")
data class TagEntry (var tag: String){
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    /* For creation with manual ID */
    @Ignore
    constructor(id: Long, tag: String): this(tag) {
        this.id = id
    }

}