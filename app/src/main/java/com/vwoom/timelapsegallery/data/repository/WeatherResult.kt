package com.vwoom.timelapsegallery.data.repository

// TODO implement weather result?
sealed class WeatherResult<out T : Any> {
    class Success<out T : Any>(val data: T) : WeatherResult<T>()

    sealed class Error(val exception: Exception?) : WeatherResult<Nothing>() {
        class NoFilesError(exception: Exception? = null,
                           val directoryUrl: String) : Error(exception)

        class InvalidCharacterError(exception: Exception? = null,
                                    val projectName: String?) : Error(exception)

        class DuplicateIdError(exception: Exception? = null,
                               val projectName: String?) : Error(exception)

        class InvalidPhotoFileError(exception: Exception? = null,
                                    val photoUrl: String,
                                    val projectName: String?) : Error(exception)

        class InvalidFolder(exception: Exception, val url: String) : Error(exception)
    }

    //object InProgress : ValidationResult<Nothing>()
}