package com.vwoom.timelapsegallery.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.vwoom.timelapsegallery.data.TimeLapseDatabase;
import com.vwoom.timelapsegallery.data.view.Project;

import java.util.List;

public class GalleryViewModel extends AndroidViewModel {

    private static final String TAG = GalleryViewModel.class.getSimpleName();

    private LiveData<List<Project>> projects;

    public GalleryViewModel(Application application) {
        super(application);
        TimeLapseDatabase database = TimeLapseDatabase.getInstance(this.getApplication());
        projects = database.projectDao().loadProjectViews();
    }

    public LiveData<List<Project>> getProjects() {
        return projects;
    }
}
