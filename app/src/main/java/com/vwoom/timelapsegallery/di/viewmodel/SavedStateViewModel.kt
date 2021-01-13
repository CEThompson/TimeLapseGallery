package com.vwoom.timelapsegallery.di.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

abstract class SavedStateViewModel: ViewModel() {

    abstract fun init(savedStateHandle: SavedStateHandle)

}