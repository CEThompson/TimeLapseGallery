package com.vwoom.timelapsegallery.data.entry

import androidx.room.*

@Entity(tableName = "photo",
        foreignKeys = [ForeignKey(entity = ProjectEntry::class,
                parentColumns = ["id"],
                childColumns = ["project_id"],
                onDelete = ForeignKey.CASCADE)])

data class PhotoEntry(
        @ColumnInfo(index = true)
        var project_id: Long,
        var timestamp: Long,
        var light: String? = null,
        var pressure: String? = null,
        var temp: String? = null,
        var humidity: String? = null) {

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    /* For inserting with auto-generated ID */
    @Ignore
    constructor(id: Long, project_id: Long, timestamp: Long) : this(project_id, timestamp) {
        this.id = id
    }

}