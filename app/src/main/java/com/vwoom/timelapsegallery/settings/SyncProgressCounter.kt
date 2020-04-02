package com.vwoom.timelapsegallery.settings

import androidx.lifecycle.MutableLiveData

object SyncProgressCounter {
    // Position update on importing projects
    var projectProgress: MutableLiveData<Int> = MutableLiveData(0)
    var projectMax: MutableLiveData<Int> = MutableLiveData(0)

    // Position update on photo number within projects
    var photoProgress: MutableLiveData<Int> = MutableLiveData(0)
    var photoMax: MutableLiveData<Int> = MutableLiveData(0)

    fun incrementProject() {
        if (projectProgress.value != null)
            projectProgress.value = projectProgress.value!! + 1
    }

    fun incrementPhoto() {
        if (photoProgress.value != null)
            photoProgress.value = photoProgress.value!! + 1
    }

    fun initProjectCount(newMax: Int) {
        projectMax.value = newMax
        projectProgress.value = 0
    }

    fun initPhotoCount(maxCount: Int) {
        photoMax.value = maxCount
        photoProgress.value = 0
    }

}