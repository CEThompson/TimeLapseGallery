package com.vwoom.timelapsegallery.data.entry

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "tag")
data class TagEntry (var text: String){
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    /* For creation with manual ID */
    @Ignore
    constructor(id: Long, text: String): this(text) {
        this.id = id
    }

}