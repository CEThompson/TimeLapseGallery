package com.vwoom.timelapsegallery.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.vwoom.timelapsegallery.database.entry.PhotoEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;

import java.util.List;

public class DetailsActivityViewModel extends ViewModel {

    private LiveData<List<PhotoEntry>> photos;

    private LiveData<ProjectEntry> currentProject;

    private final static String TAG = DetailsActivityViewModel.class.getSimpleName();

    public DetailsActivityViewModel(TimeLapseDatabase database, long projectId){
        photos = database.photoDao().loadAllPhotosByProjectId(projectId);
        currentProject = database.projectDao().loadLiveDataProjectById(projectId);
    }

    public LiveData<List<PhotoEntry>> getPhotos(){ return photos;}

    public LiveData<ProjectEntry> getCurrentProject() { return currentProject; }

}
