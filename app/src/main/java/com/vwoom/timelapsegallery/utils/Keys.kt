package com.vwoom.timelapsegallery.utils

import com.vwoom.timelapsegallery.BuildConfig

object Keys {
    /* Project Keys */
    const val PROJECT_FILTER_BY_SCHEDULED_TODAY = "filter_by_scheduled_today"
    /* Notifications */
    const val REQUEST_CODE = "request_code"
    /* Authorities */
    private const val applicationId = BuildConfig.APPLICATION_ID
    const val CREATE_NOTIFICATION_AUTHORITY = "$applicationId.CREATE_NOTIFICATION"
}