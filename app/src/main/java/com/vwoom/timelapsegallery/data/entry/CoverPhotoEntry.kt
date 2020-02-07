package com.vwoom.timelapsegallery.data.entry

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "cover_photo",
        primaryKeys = ["project_id"],
        foreignKeys = [ForeignKey(entity = ProjectEntry::class,
                parentColumns = ["id"],
                childColumns = ["project_id"],
                onDelete = ForeignKey.CASCADE),
            ForeignKey(entity = PhotoEntry::class,
                    parentColumns = ["id"],
                    childColumns = ["photo_id"],
                    onDelete = ForeignKey.CASCADE)])
data class CoverPhotoEntry(
        @ColumnInfo(index = true)
        var project_id: Long,
        @ColumnInfo(index = true)
        var photo_id: Long)