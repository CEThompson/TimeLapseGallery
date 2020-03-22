package com.vwoom.timelapsegallery.settings

import androidx.lifecycle.MutableLiveData


object SyncProgressCounter {
    var progress: MutableLiveData<Int> = MutableLiveData(0)
    var max: MutableLiveData<Int> = MutableLiveData(0)

    fun increment() {
        if (progress.value!=null)
            progress.value = progress.value!! + 1
    }

    fun setMax(newMax: Int){
        max.value = newMax
    }
}