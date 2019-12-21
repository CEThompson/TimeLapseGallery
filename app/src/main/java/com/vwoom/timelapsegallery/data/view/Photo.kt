package com.vwoom.timelapsegallery.data.view

import android.os.Parcelable
import androidx.room.DatabaseView
import kotlinx.android.parcel.Parcelize

// TODO implement last photo view for project
@DatabaseView("SELECT " +
        "project.id AS project_id, " +
        "project.project_name AS project_name, " +
        "project.cover_set_by_user AS cover_set_by_user, " +
        "project_schedule.schedule_time AS schedule_time, " +
        "project_schedule.interval_days AS interval_days, " +
        "cover_photo.photo_id AS cover_photo_id, " +
        "photo.timestamp AS cover_photo_timestamp " +
        "FROM project " +
        "LEFT JOIN project_schedule ON project.id = project_schedule.project_id " +
        "LEFT JOIN cover_photo ON project.id = cover_photo.project_id " +
        "LEFT JOIN photo ON cover_photo.photo_id = photo.id")
@Parcelize
data class Photo (
    val photo_id: Long,
    val photo_timestamp: Long,
    val photo_url: String
) : Parcelable