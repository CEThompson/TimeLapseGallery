package com.vwoom.timelapsegallery.utils

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

// TODO: (deferred) assess locales for time
// NOTE: Timestamps are in milliseconds
class TimeUtilsTest {

    @Test
    fun getDaysSinceTimeStamp_checksIntervalIndependentOfHours() {
        // Given two timestamps, where now is two days after the photo both in the same year
        val calendar = Calendar.getInstance()

        // 3-11-2020 (wednesday) @ midnight
        calendar[Calendar.MONTH] = Calendar.MARCH
        calendar[Calendar.DAY_OF_MONTH] = 11
        calendar[Calendar.YEAR] = 2020
        calendar[Calendar.HOUR_OF_DAY] = 23
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)
        calendar.clear(Calendar.MILLISECOND)
        var timestampPhoto: Long = calendar.timeInMillis

        // 3-13-2020 (friday)
        calendar[Calendar.DAY_OF_MONTH] = 12
        calendar[Calendar.HOUR_OF_DAY] = 0
        var timestampNow: Long = calendar.timeInMillis

        // When we call the utility class
        var daysSinceTimestamp: Long = TimeUtils.getDaysSinceTimeStamp(timestampPhoto, timestampNow)

        // Then the days returned should be two
        println("days since is $daysSinceTimestamp")
        assertTrue(daysSinceTimestamp == 1.toLong())

        // 3-11-2020 (wednesday) @ midnight
        calendar[Calendar.MONTH] = Calendar.MARCH
        calendar[Calendar.DAY_OF_MONTH] = 11
        calendar[Calendar.YEAR] = 2020
        calendar[Calendar.HOUR_OF_DAY] = 0
        timestampPhoto = calendar.timeInMillis

        // 3-13-2020 (friday)
        calendar[Calendar.DAY_OF_MONTH] = 12
        calendar[Calendar.HOUR_OF_DAY] = 22
        calendar[Calendar.MINUTE] = 50
        timestampNow = calendar.timeInMillis

        // When we call the utility class
        daysSinceTimestamp = TimeUtils.getDaysSinceTimeStamp(timestampPhoto, timestampNow)

        // Then the days returned should be two
        println("days since is $daysSinceTimestamp")
        assertTrue(daysSinceTimestamp == 1.toLong())
    }

    @Test
    fun getDaysSinceTimeStamp_sameYear() {
        // Given two timestamps, where now is two days after the photo both in the same year
        val calendar = Calendar.getInstance()

        calendar[Calendar.MONTH] = Calendar.MARCH
        calendar[Calendar.DAY_OF_MONTH] = 11
        calendar[Calendar.YEAR] = 2020
        calendar[Calendar.HOUR_OF_DAY] = 11 // hour starts a 0 thus noon
        val timestampPhoto: Long = calendar.timeInMillis // 3-11-2020 (wednesday) @ noon

        calendar[Calendar.DAY_OF_MONTH] = 13
        calendar[Calendar.HOUR_OF_DAY] = 0
        val timestampNow: Long = calendar.timeInMillis // 3-13-2020 (friday)

        // When we call the utility class
        val daysSinceTimestamp: Long = TimeUtils.getDaysSinceTimeStamp(timestampPhoto, timestampNow)

        // Then the days returned should be two
        println("days since is $daysSinceTimestamp")
        assertTrue(daysSinceTimestamp == 2.toLong())
    }

    @Test
    fun getDaysSinceTimestamp_differentYear() {
        // Given two timestamps, now is 4 days after the photo but in a new year
        val calendar = Calendar.getInstance()
        calendar[Calendar.MONTH] = Calendar.DECEMBER
        calendar[Calendar.DAY_OF_MONTH] = 30
        calendar[Calendar.HOUR_OF_DAY] = 20
        calendar[Calendar.YEAR] = 2019
        val timestampPhoto = calendar.timeInMillis // 12-30-2019 (monday)
        calendar[Calendar.MONTH] = Calendar.JANUARY
        calendar[Calendar.DAY_OF_MONTH] = 3
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.YEAR] = 2020
        val timestampNow = calendar.timeInMillis // 1-3-2020 (friday)

        // When we call the utility class
        val daysSinceTimestamp = TimeUtils.getDaysSinceTimeStamp(timestampPhoto, timestampNow)

        // Then the days since the timestamp should be four
        println("days since is $daysSinceTimestamp")
        assertTrue(daysSinceTimestamp == 4.toLong())
    }

    // Tests a multi year interval with a leap year (2020)
    @Test
    fun getDaysSinceTimestamp_leapYearAccountedFor() {
        // Given two timestamps, now is 4 days after the photo but in a new year
        val calendar = Calendar.getInstance()
        calendar[Calendar.MONTH] = Calendar.DECEMBER
        calendar[Calendar.DAY_OF_MONTH] = 30
        calendar[Calendar.HOUR_OF_DAY] = 20
        calendar[Calendar.YEAR] = 2019
        val timestampPhoto = calendar.timeInMillis // 12-30-2019 (monday)
        calendar[Calendar.MONTH] = Calendar.JANUARY
        calendar[Calendar.DAY_OF_MONTH] = 3
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.YEAR] = 2021
        val timestampNow = calendar.timeInMillis // 1-3-2020 (friday)

        // When we call the utility class
        val daysSinceTimestamp = TimeUtils.getDaysSinceTimeStamp(timestampPhoto, timestampNow)

        // Then the days since the timestamp should be 370
        // 2020 leap year = 366
        // days in 2019 = 1
        // days in 2021 = 3
        println("days since is $daysSinceTimestamp")
        assertTrue(daysSinceTimestamp == 370.toLong())
    }

    // Tests a multiple year interval including both leap year and non-leap year
    @Test
    fun getDaysSinceTimestamp_multiYearInterval_withOneLeapYear() {
        // Given two timestamps one at the end of 2019 and one 3 days into 2022
        val calendar = Calendar.getInstance()
        calendar[Calendar.MONTH] = Calendar.DECEMBER
        calendar[Calendar.DAY_OF_MONTH] = 30
        calendar[Calendar.YEAR] = 2019
        calendar[Calendar.HOUR_OF_DAY] = 20
        val timestampPhoto = calendar.timeInMillis // 12-30-2019 (monday)
        calendar[Calendar.MONTH] = Calendar.JANUARY
        calendar[Calendar.DAY_OF_MONTH] = 3
        calendar[Calendar.YEAR] = 2022
        calendar[Calendar.HOUR_OF_DAY] = 0
        val timestampNow = calendar.timeInMillis // 1-3-2020 (friday)

        // When we call the utility class
        val daysSinceTimestamp = TimeUtils.getDaysSinceTimeStamp(timestampPhoto, timestampNow)

        // Then the days since the timestamp should be 735
        // days in 2019 = 1
        // leap year 2020 = 366
        // leap year 2021 = 365
        // days in 2022 = 3
        // for a total of 735 days

        println("days since is $daysSinceTimestamp")
        assertTrue(daysSinceTimestamp == 735.toLong())
    }

    // Tests a multiple year interval without leap years
    @Test
    fun getDaysSinceTimestamp_multiYearInterval_withoutLeapYears() {
        // Given two timestamps one at the end of 2019 and one 3 days into 2022
        val calendar = Calendar.getInstance()
        calendar[Calendar.MONTH] = Calendar.DECEMBER
        calendar[Calendar.DAY_OF_MONTH] = 30
        calendar[Calendar.YEAR] = 2021
        calendar[Calendar.HOUR_OF_DAY] = 20
        val timestampPhoto = calendar.timeInMillis // 12-30-2019 (monday)
        calendar[Calendar.MONTH] = Calendar.JANUARY
        calendar[Calendar.DAY_OF_MONTH] = 3
        calendar[Calendar.YEAR] = 2023
        calendar[Calendar.HOUR_OF_DAY] = 0
        val timestampNow = calendar.timeInMillis // 1-3-2020 (friday)

        // When we call the utility class
        val daysSinceTimestamp = TimeUtils.getDaysSinceTimeStamp(timestampPhoto, timestampNow)

        // Then the days since the timestamp should be 735
        // days in 2021 = 1
        // non-leap year 2022 = 365
        // days in 2023 = 3
        // for a total of 369 days

        println("days since is $daysSinceTimestamp")
        assertTrue(daysSinceTimestamp == 369.toLong())
    }

    @Test
    fun getDaysSinceTimestamp_currentTimeBeforeTimestamp() {
        // Given two timestamps, the current is before the photo
        val calendar = Calendar.getInstance()
        calendar[Calendar.MONTH] = Calendar.DECEMBER
        calendar[Calendar.DAY_OF_MONTH] = 30
        calendar[Calendar.YEAR] = 2020
        calendar[Calendar.HOUR_OF_DAY] = 20
        val timestampPhoto = calendar.timeInMillis // 12-30-2019 (monday)
        calendar[Calendar.MONTH] = Calendar.JANUARY
        calendar[Calendar.DAY_OF_MONTH] = 3
        calendar[Calendar.YEAR] = 2020
        calendar[Calendar.HOUR_OF_DAY] = 0
        val timestampNow = calendar.timeInMillis // 1-3-2020 (friday)

        // When we call the utility class
        val daysSinceTimestamp = TimeUtils.getDaysSinceTimeStamp(timestampPhoto, timestampNow)

        // Then the days since the timestamp should return a -1 error indication
        println("days since is $daysSinceTimestamp")
        assertTrue(daysSinceTimestamp == -(1.toLong()))
    }

    @Test
    fun getTimestampStartOfDay(){
        // Given a timestamp
        //Timestamp in milliseconds: 1584057600000
        //Date and time (GMT): Friday, March 13, 2020 12:00:00 AM
        //Date and time (your time zone): Thursday, March 12, 2020 5:00:00 PM GMT-07:00
        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar[Calendar.MONTH] = Calendar.MARCH
        calendar[Calendar.DAY_OF_MONTH] = 13
        calendar[Calendar.YEAR] = 2020
        calendar[Calendar.HOUR] = 0
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        val startOfDayTimestamp = calendar.timeInMillis

        calendar[Calendar.MONTH] = Calendar.MARCH
        calendar[Calendar.DAY_OF_MONTH] = 13
        calendar[Calendar.YEAR] = 2020
        calendar[Calendar.HOUR_OF_DAY] = 15
        calendar[Calendar.SECOND] = 16
        val timestampDuringDay = calendar.timeInMillis

        // When we call the function
        val calculatedStartOfDay = TimeUtils.getTimestampStartOfDay(timestampDuringDay)

        // Then the timestamp is from the start of the day
        println("start of day value is $startOfDayTimestamp")
        println("calculated value from function is $calculatedStartOfDay")
        assertTrue(calculatedStartOfDay == startOfDayTimestamp)
    }
}