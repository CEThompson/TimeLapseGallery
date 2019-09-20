package com.vwoom.timelapsecollection.utils;

import android.text.format.DateUtils;

import com.vwoom.timelapsecollection.database.entry.ProjectEntry;

import java.util.ArrayList;
import java.util.List;

public class ProjectUtils {
    /* Takes all scheduled projects and returns a list of projects scheduled for today */
    public static List<ProjectEntry> getProjectsScheduledToday(List<ProjectEntry> projects){
        List<ProjectEntry> projectsForToday = new ArrayList<>();

        /* Go through all scheduled projects and create a list of projects scheduled for today */
        for (ProjectEntry currentProject : projects){

            // Find the next scheduled time
            long nextScheduledPhotoTimestamp = currentProject.getSchedule_next_submission();
            int schedule = currentProject.getSchedule();

            // If that timestamp belongs to today, add it to the list
            // If the timestamp is due up, add the project to the list as well
            if (schedule != 0
                    && (DateUtils.isToday(nextScheduledPhotoTimestamp) || System.currentTimeMillis() > nextScheduledPhotoTimestamp))
                projectsForToday.add(currentProject);
        }

        // Return the list of todays scheduled projects
        return projectsForToday;
    }
}
