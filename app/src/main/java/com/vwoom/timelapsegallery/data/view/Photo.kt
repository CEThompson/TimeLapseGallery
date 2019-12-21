package com.vwoom.timelapsegallery.data.view

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

// TODO figure out proper abstraction for passing a photo
@Parcelize
data class Photo (
    val photo_id: Long,
    val project_id: Long,
    val photo_timestamp: Long,
    val photo_url: String
) : Parcelable