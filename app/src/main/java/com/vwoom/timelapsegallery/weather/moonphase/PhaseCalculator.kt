package com.vwoom.timelapsegallery.weather.moonphase

import java.util.*

// TODO: (deferred) handle phases of the moon in a moon phase dialog
// TODO: (deferred) handle northern vs southern hemisphere representations, localization for moon phases

object PhaseCalculator {

    // Calendar value for a synodic month which is 29.530587981 days
    // Calculated using 29.53
    private const val synodicMonthDays = 29 //29.53 days
    private const val synodicMonthHours = 12 //12.72 hours
    private const val synodicMonthMin = 43  // 43.2 seconds
    private const val synodicMonthSec = 12 // .2 of a min is 12 seconds

    // reference timestamp for new moon
    // April 22, 2020 7:25 PM PST
    private const val newMoonReference: Long = 1587608700000

    // Ref timestamp for full moon
    // May 7, 2020 3:45 AM PST
    private const val fullMoonReference: Long = 1588848300000

    fun getMoonPhaseFromTimestamp(timestamp: Long): PhaseType {
        // Calc newMoon prev to timestamp
        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar.timeInMillis = fullMoonReference

        // Get the timestamp for the day of the next full moon
        while (calendar.timeInMillis < timestamp) {
            calendar.add(Calendar.DAY_OF_YEAR, synodicMonthDays)
            calendar.add(Calendar.HOUR_OF_DAY, synodicMonthHours)
            calendar.add(Calendar.MINUTE, synodicMonthMin)
            calendar.add(Calendar.SECOND, synodicMonthSec)
        }

        val nextFullMoonTimestamp = calendar.timeInMillis
        val prevNewMoonTimeStamp = nextFullMoonTimestamp - (fullMoonReference - newMoonReference)

        calendar.timeInMillis = prevNewMoonTimeStamp

        val newMoonYear = calendar.get(Calendar.YEAR)
        val newMoonDay = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = timestamp

        val comparisonYear = calendar.get(Calendar.YEAR)
        val comparisonDay = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = nextFullMoonTimestamp

        val fullMoonYear = calendar.get(Calendar.YEAR)
        val fullMoonDay = calendar.get(Calendar.DAY_OF_YEAR)

        // Timestamp is new moon
        return if (newMoonYear == comparisonYear && newMoonDay == comparisonDay) {
            PhaseType.NEW_MOON
        } else if (fullMoonYear == comparisonYear && fullMoonDay == comparisonDay) {
            PhaseType.FULL_MOON
        } else PhaseType.INTERMEDIATE
    }
}