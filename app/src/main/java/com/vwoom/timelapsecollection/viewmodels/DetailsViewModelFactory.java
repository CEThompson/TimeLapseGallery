package com.vwoom.timelapsecollection.viewmodels;

import android.util.Log;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.vwoom.timelapsecollection.database.TimeLapseDatabase;

public class DetailsViewModelFactory extends ViewModelProvider.NewInstanceFactory {

    private final TimeLapseDatabase mTimeLapseDb;
    private final long mProjectId;

    private static final String TAG = DetailsViewModelFactory.class.getSimpleName();

    public DetailsViewModelFactory(TimeLapseDatabase timeLapseDatabase, long projectId){
        this.mTimeLapseDb = timeLapseDatabase;
        this.mProjectId = projectId;

        Log.d(TAG, "viewmodel factory firing up project id is " + projectId);
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass){
        return (T) new DetailsActivityViewModel(mTimeLapseDb, mProjectId);
    }

}
