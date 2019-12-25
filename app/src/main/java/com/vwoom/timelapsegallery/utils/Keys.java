package com.vwoom.timelapsegallery.utils;

import com.vwoom.timelapsegallery.BuildConfig;

public final class Keys {
    /* Project Keys */
    public static final String PROJECT = "project";
    public static final String PROJECT_NAME = "project_name";
    public static final String PROJECT_SCHEDULE = "project_schedule";
    public static final String PROJECT_ENTRY = "project_entry";
    public static final String PROJECT_FILTER_BY_SCHEDULED_TODAY = "filter_by_scheduled_today";
    public static final String PROJECT_SCHEDULE_ENTRY = "project_schedule_entry";
    public static final String COVER_PHOTO_ENTRY = "cover_photo_entry";

    /* Photo Keys */
    public static final String PHOTO_ENTRY = "photo_entry";
    public static final String PHOTO_PATH = "photo_path";
    public static final String TEMP_PATH = "temp_path";

    /* Notifications */
    public static final String CHOSEN_TIME = "chosen_time";
    public static final String NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String REQUEST_CODE = "request_code";

    /* Shared element transition */
    public static final String TRANSITION_NAME = "transition_name";
    public static final String TRANSITION_POSITION = "transition_position";

    /* Authorities */
    private static final String applicationId = BuildConfig.APPLICATION_ID;

    public static final String FILEPROVIDER_AUTHORITY = applicationId + ".fileprovider";
    public static final String CREATE_NOTIFICATION_AUTHORITY = applicationId + ".CREATE_NOTIFICATION";
}