package com.vwoom.timelapsegallery.utils

import org.junit.Test
import java.util.*

// TODO assess locales for time
// NOTE: Timestamps are in milliseconds
class TimeUtilsTest {

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
        assert(daysSinceTimestamp == 2.toLong())
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
        assert(daysSinceTimestamp == 4.toLong())
    }

    @Test
    fun getDaysSinceTimestamp_manyYearInterval() {
        // Given two timestamps, now is 4 days after the photo but in a new year
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

        // Then the days since the timestamp should be four
        println("days since is $daysSinceTimestamp")
        assert(daysSinceTimestamp != 4.toLong())
    }

    // TODO create a test case for a many year interval testing leap year
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
        assert(daysSinceTimestamp == -(1.toLong()))
    }
}