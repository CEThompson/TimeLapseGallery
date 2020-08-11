package com.vwoom.timelapsegallery.location

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import timber.log.Timber

object SingleShotLocationProvider {

    private val criteria = object : Criteria() {
        override fun getAccuracy(): Int {
            return ACCURACY_FINE
        }
    }

    interface LocationCallback {
        fun onNewLocationAvailable(location: Location?)
    }

    fun requestSingleUpdate(context: Context, callback: LocationCallback) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        request(locationManager, callback)
    }

    private fun request(locationManager: LocationManager, callback: LocationCallback) {
        try {
            locationManager.requestSingleUpdate(criteria, object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    callback.onNewLocationAvailable(location)
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }, null)
        } catch (e: SecurityException) {
            Timber.d("Single shot location request failed. Reason: ${e.message}")
        }
    }

}