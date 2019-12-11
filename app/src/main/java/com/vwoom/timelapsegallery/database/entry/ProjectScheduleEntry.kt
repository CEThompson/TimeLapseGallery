package com.vwoom.timelapsegallery.database.entry

import androidx.room.Entity
import androidx.room.ForeignKey
import com.vwoom.timelapsegallery.database.entry.ProjectEntry

@Entity(tableName = "project_schedule",
        primaryKeys = ["project_id"],
        foreignKeys = [ForeignKey(entity = ProjectEntry::class,
                parentColumns = ["id"],
                childColumns = ["project_id"],
                onDelete = ForeignKey.CASCADE)])
data class ProjectScheduleEntry(var project_id: Long, var schedule_time: Long?, var interval_days: Int?)