package com.ajesh.syncspend

import com.ajesh.syncspend.ui.components.rangePresets
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomRangePresetsTest {
    private val today = LocalDate.of(2026, 9, 27)

    @Test fun offersSixtyNinetyAndSixMonthsEndingToday() {
        val presets = rangePresets(today)
        assertEquals(listOf("Last 60 days", "Last 90 days", "Last 6 months"), presets.map { it.first })
        assertEquals(true, presets.all { it.second.end == today })
    }

    @Test fun dayLookBacksCountTheStatedNumberOfDaysInclusive() {
        val presets = rangePresets(today).toMap()
        assertEquals(60, presets.getValue("Last 60 days").dayCount)
        assertEquals(90, presets.getValue("Last 90 days").dayCount)
        assertEquals(LocalDate.of(2026, 7, 30), presets.getValue("Last 60 days").start)
    }

    @Test fun sixMonthsStartsTheDayAfterSixMonthsAgo() {
        assertEquals(LocalDate.of(2026, 3, 28), rangePresets(today).toMap().getValue("Last 6 months").start)
    }
}
