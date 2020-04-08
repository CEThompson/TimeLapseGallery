package com.vwoom.timelapsegallery.data.view

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.android.parcel.Parcelize

@Keep
@Parcelize
data class Photo (
    val photo_id: Long,
    val project_id: Long,
    val photo_timestamp: Long,
    val photo_url: String
) : Parcelable