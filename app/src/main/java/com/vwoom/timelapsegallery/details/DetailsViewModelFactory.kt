package com.vwoom.timelapsegallery.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import com.vwoom.timelapsegallery.data.TimeLapseDatabase

class DetailsViewModelFactory(private val mTimeLapseDb: TimeLapseDatabase, private val mProjectId: Long) : NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return DetailsActivityViewModel(mTimeLapseDb, mProjectId) as T
    }

    companion object {
        private val TAG = DetailsViewModelFactory::class.java.simpleName
    }

    init {
        Log.d(TAG, "viewmodel factory firing up project id is $mProjectId")
    }
}