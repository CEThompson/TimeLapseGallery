package com.vwoom.timelapsegallery.data.view

import android.os.Parcelable
import androidx.room.DatabaseView
import kotlinx.android.parcel.Parcelize

@DatabaseView("SELECT " +
        "project.id AS project_id, " +
        "project.project_name AS project_name, " +
        "project_schedule.interval_days AS interval_days, " +
        "cover_photo.photo_id AS cover_photo_id, " +
        "photo.timestamp AS cover_photo_timestamp " +
        "FROM project " +
        "LEFT JOIN project_schedule ON project.id = project_schedule.project_id " +
        "LEFT JOIN cover_photo ON project.id = cover_photo.project_id " +
        "LEFT JOIN photo ON cover_photo.photo_id = photo.id")
@Parcelize
data class Project(
        val project_id: Long,
        val project_name: String?,
        val interval_days: Int,
        val cover_photo_id: Long,
        val cover_photo_timestamp: Long
) : Parcelable
