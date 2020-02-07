package com.vwoom.timelapsegallery.data.entry

import androidx.room.*

// TODO verify cascade deletion works appropriately
@Entity(tableName = "project_tag",
        foreignKeys = [
        ForeignKey(entity = ProjectEntry::class,
                parentColumns = ["id"],
                childColumns = ["project_id"],
                onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntry::class,
                parentColumns = ["id"],
                childColumns = ["tag_id"],
                onDelete = ForeignKey.CASCADE)
        ])
data class ProjectTagEntry(
        @ColumnInfo(index = true)
        var project_id: Long,
        @ColumnInfo(index = true)
        var tag_id: Long){
        @PrimaryKey(autoGenerate = true)
        var id: Long = 0
}