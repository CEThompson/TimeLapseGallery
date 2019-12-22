package com.vwoom.timelapsegallery.data.entry

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
data class CoverPhotoEntry(var project_id: Long, var photo_id: Long)