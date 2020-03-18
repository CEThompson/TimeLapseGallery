package com.vwoom.timelapsegallery.settings

sealed class ValidationResult<out T : Any> {
    class Success<out T : Any> : ValidationResult<T>()
    sealed class Error(val exception: Exception?) : ValidationResult<Nothing>() {
        class NoFilesError(exception: Exception? = null,
                           val directoryUrl: String) : Error(exception)

        class InvalidCharacterError(exception: Exception? = null,
                                    val projectName: String?) : Error(exception)

        class DuplicateIdError(exception: Exception? = null,
                               val projectName: String?) : Error(exception)

        class InvalidPhotoFileError(exception: Exception? = null,
                                    val photoUrl: String,
                                    val projectName: String?) : Error(exception)
        class InvalidFolder(exception: Exception, val url: String): Error(exception)
    }

    object InProgress : ValidationResult<Nothing>()
}