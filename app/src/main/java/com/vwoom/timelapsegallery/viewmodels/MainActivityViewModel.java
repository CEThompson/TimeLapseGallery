package com.vwoom.timelapsegallery.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.vwoom.timelapsegallery.database.entry.CoverPhotoEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.database.entry.ProjectScheduleEntry;

import java.util.List;

public class MainActivityViewModel extends AndroidViewModel {

    private static final String TAG = MainActivityViewModel.class.getSimpleName();

    private LiveData<List<ProjectEntry>> projects;
    private LiveData<List<CoverPhotoEntry>> projectCovers;
    private LiveData<List<ProjectScheduleEntry>> projectSchedules;

    public MainActivityViewModel(Application application) {
        super(application);
        TimeLapseDatabase database = TimeLapseDatabase.getInstance(this.getApplication());
        projects = database.projectDao().loadAllProjects();
        projectCovers = database.coverPhotoDao().getAllCoverPhotos();
        projectSchedules = database.projectScheduleDao().loadProjectSchedules();
    }

    public LiveData<List<ProjectEntry>> getProjects() {
        return projects;
    }

    public LiveData<List<CoverPhotoEntry>> getProjectCovers() {
        return projectCovers;
    }

    public LiveData<List<ProjectScheduleEntry>> getProjectSchedules() {
        return projectSchedules;
    }
}
