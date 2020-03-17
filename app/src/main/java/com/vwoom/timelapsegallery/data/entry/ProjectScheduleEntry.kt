package com.vwoom.timelapsegallery.data.entry

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "project_schedule",
        primaryKeys = ["project_id"],
        foreignKeys = [ForeignKey(entity = ProjectEntry::class,
                parentColumns = ["id"],
                childColumns = ["project_id"],
                onDelete = ForeignKey.CASCADE)])
data class ProjectScheduleEntry(var project_id: Long, var interval_days: Int)