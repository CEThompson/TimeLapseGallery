package com.vwoom.timelapsegallery.testing

import androidx.test.espresso.IdlingResource

// Singleton resource for idling tests
object EspressoIdlingResource {
    private const val resource = "GLOBAL"
    private val countingIdlingResource = SimpleCountingIdlingResource(resource)
    fun increment() = countingIdlingResource.increment()
    fun decrement() = countingIdlingResource.decrement()
    fun getIdlingResource(): IdlingResource = countingIdlingResource
}