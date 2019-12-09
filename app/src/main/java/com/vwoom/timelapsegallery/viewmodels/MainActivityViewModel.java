package com.vwoom.timelapsegallery.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.database.view.Project;

import java.util.List;

public class MainActivityViewModel extends AndroidViewModel {

    private static final String TAG = MainActivityViewModel.class.getSimpleName();

    private LiveData<List<Project>> projects;

    public MainActivityViewModel(Application application) {
        super(application);
        TimeLapseDatabase database = TimeLapseDatabase.getInstance(this.getApplication());
        projects = database.projectDao().loadProjectsWithInfo();
    }

    public LiveData<List<Project>> getProjects() {
        return projects;
    }
}
