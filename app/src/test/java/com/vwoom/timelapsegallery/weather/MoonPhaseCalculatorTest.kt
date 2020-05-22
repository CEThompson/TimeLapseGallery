package com.vwoom.timelapsegallery.weather

import org.junit.Test

import java.util.*

class MoonPhaseCalculatorTest {
    // Dec 3, 2040 11:33 PM PST should be new moon
    private val timestampNewMoon = 2238219180000
    // Dec 18, 2040 4:15 AM PST should be full moon
    private val timestampFullMoon = 2239445640000

    private val millisecondsDay = 86400000

    @Test
    fun getMoonPhaseFromTimestamp_shouldBeNewMoon() {
        val phase = MoonPhaseCalculator.getMoonPhaseFromTimestamp(timestampNewMoon)

        println(phase)
        assert(phase == NEW_MOON)
    }

    @Test
    fun getMoonPhaseFromTimestamp_shouldBeFullMoon() {
        val phase = MoonPhaseCalculator.getMoonPhaseFromTimestamp(timestampFullMoon)
        println(phase)
        assert(phase == FULL_MOON)
    }

    @Test
    fun getMoonPhaseFromTimestamp_shouldBeIntermediate() {
        var timeBefore = timestampNewMoon - millisecondsDay
        var timeAfter = timestampNewMoon + millisecondsDay

        // Check the day after and day before new moon
        var phaseBefore = MoonPhaseCalculator.getMoonPhaseFromTimestamp(timeBefore)
        var phaseAfter = MoonPhaseCalculator.getMoonPhaseFromTimestamp(timeAfter)
        println(phaseBefore)
        assert(phaseBefore == INTERMEDIATE_MOON)
        println(phaseAfter)
        assert(phaseAfter == INTERMEDIATE_MOON)

        timeBefore = timestampFullMoon - millisecondsDay
        timeAfter = timestampNewMoon + millisecondsDay

        phaseBefore = MoonPhaseCalculator.getMoonPhaseFromTimestamp(timeBefore)
        phaseAfter = MoonPhaseCalculator.getMoonPhaseFromTimestamp(timeAfter)

        println(phaseBefore)
        assert(phaseBefore == INTERMEDIATE_MOON)
        println(phaseAfter)
        assert(phaseAfter == INTERMEDIATE_MOON)
    }

}