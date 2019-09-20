package com.vwoom.timelapsecollection.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.vwoom.timelapsecollection.database.entry.ProjectEntry;
import com.vwoom.timelapsecollection.database.TimeLapseDatabase;

import java.util.List;

public class MainActivityViewModel extends AndroidViewModel {

    private static final String TAG = MainActivityViewModel.class.getSimpleName();

    private LiveData<List<ProjectEntry>> projects;

    public MainActivityViewModel(Application application){
        super(application);
        TimeLapseDatabase database = TimeLapseDatabase.getInstance(this.getApplication());
        projects = database.projectDao().loadAllProjects();
    }

    public LiveData<List<ProjectEntry>> getProjects() { return projects; }

}
