package com.vwoom.timelapsegallery.weather.moonphase


import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseCalculatorTest {
    // Dec 3, 2040 11:33 PM PST should be new moon
    private val timestampNewMoon = 2238219180000
    // Dec 18, 2040 4:15 AM PST should be full moon
    private val timestampFullMoon = 2239445640000

    private val millisecondsDay = 86400000

    @Test
    fun getMoonPhaseFromTimestamp_shouldBeNewMoon() {
        val phase = PhaseCalculator.getMoonPhaseFromTimestamp(timestampNewMoon)

        println(phase)
        assertTrue(phase == PhaseType.NEW_MOON)
    }

    @Test
    fun getMoonPhaseFromTimestamp_shouldBeFullMoon() {
        val phase = PhaseCalculator.getMoonPhaseFromTimestamp(timestampFullMoon)
        println(phase)
        assertTrue(phase == PhaseType.FULL_MOON)
    }

    @Test
    fun getMoonPhaseFromTimestamp_shouldBeIntermediate() {
        var timeBefore = timestampNewMoon - millisecondsDay
        var timeAfter = timestampNewMoon + millisecondsDay

        // Check the day after and day before new moon
        var phaseBefore = PhaseCalculator.getMoonPhaseFromTimestamp(timeBefore)
        var phaseAfter = PhaseCalculator.getMoonPhaseFromTimestamp(timeAfter)
        println(phaseBefore)
        assertTrue(phaseBefore == PhaseType.INTERMEDIATE)
        println(phaseAfter)
        assertTrue(phaseAfter == PhaseType.INTERMEDIATE)

        timeBefore = timestampFullMoon - millisecondsDay
        timeAfter = timestampNewMoon + millisecondsDay

        phaseBefore = PhaseCalculator.getMoonPhaseFromTimestamp(timeBefore)
        phaseAfter = PhaseCalculator.getMoonPhaseFromTimestamp(timeAfter)

        println(phaseBefore)
        assertTrue(phaseBefore == PhaseType.INTERMEDIATE)
        println(phaseAfter)
        assertTrue(phaseAfter == PhaseType.INTERMEDIATE)
    }

}