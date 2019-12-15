package com.vwoom.timelapsegallery.data.entry

import androidx.room.Entity
import androidx.room.ForeignKey

// TODO verify cascade deletion works appropriately
// TODO set index for columns
@Entity(tableName = "project_tag",
        primaryKeys = ["project_id"],
        foreignKeys = [ForeignKey(entity = ProjectEntry::class,
                parentColumns = ["id"],
                childColumns = ["project_id"],
                onDelete = ForeignKey.CASCADE),
            ForeignKey(entity = TagEntry::class,
                    parentColumns = ["id"],
                    childColumns = ["tag_id"],
                    onDelete = ForeignKey.CASCADE)])
data class ProjectTagEntry(var project_id: Long, var tag_id: Long)