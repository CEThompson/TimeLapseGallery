package com.vwoom.timelapsegallery.data.source

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object MoshiHelper {
    val instance by lazy {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }
}