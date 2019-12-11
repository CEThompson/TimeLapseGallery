package com.vwoom.timelapsegallery.database.entry

import androidx.room.Entity
import androidx.room.ForeignKey
import com.vwoom.timelapsegallery.database.entry.PhotoEntry
import com.vwoom.timelapsegallery.database.entry.ProjectEntry

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
class CoverPhotoEntry(// Setters
        // Getters
        var project_id: Long, var photo_id: Long)