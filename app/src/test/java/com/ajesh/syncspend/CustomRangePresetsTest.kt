package com.ajesh.syncspend

import com.ajesh.syncspend.ui.components.rangePresets
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

/** The Custom range shortcuts: whole calendar months back from today, greyed out until the entries reach that far. */
class CustomRangePresetsTest {
    private val today = LocalDate.of(2026, 9, 27)
    private val oldest = YearMonth.of(2020, 1)

    @Test fun offersTwoThreeAndSixMonthsEndingToday() {
        val presets = rangePresets(today, oldest)
        assertEquals(listOf("Last 2 months", "Last 3 months", "Last 6 months"), presets.map { it.label })
        assertEquals(true, presets.all { it.range.end == today })
    }

    @Test fun eachStartsOnTheFirstOfItsFirstMonth() {
        val byLabel = rangePresets(today, oldest).associate { it.label to it.range.start }
        assertEquals(LocalDate.of(2026, 8, 1), byLabel.getValue("Last 2 months"))
        assertEquals(LocalDate.of(2026, 7, 1), byLabel.getValue("Last 3 months")) // Sep 27: Jul 1 to Sep 27
        assertEquals(LocalDate.of(2026, 4, 1), byLabel.getValue("Last 6 months"))
    }

    @Test fun startsFollowTheMonthNotADayCount() {
        val oct3 = rangePresets(LocalDate.of(2026, 10, 3), oldest).associate { it.label to it.range.start }
        assertEquals(LocalDate.of(2026, 9, 1), oct3.getValue("Last 2 months"))
        assertEquals(LocalDate.of(2026, 8, 1), oct3.getValue("Last 3 months"))
        // Across a year boundary.
        val jan = rangePresets(LocalDate.of(2027, 1, 15), oldest).associate { it.label to it.range.start }
        assertEquals(LocalDate.of(2026, 11, 1), jan.getValue("Last 3 months"))
        assertEquals(LocalDate.of(2026, 8, 1), jan.getValue("Last 6 months"))
    }

    @Test fun aPresetIsOnlyEnabledOnceTheEntriesReachItsFirstMonth() {
        fun enabled(lower: YearMonth) = rangePresets(today, lower).filter { it.enabled }.map { it.label }
        assertEquals(emptyList<String>(), enabled(YearMonth.of(2026, 9))) // no history yet
        assertEquals(listOf("Last 2 months"), enabled(YearMonth.of(2026, 8)))
        assertEquals(listOf("Last 2 months", "Last 3 months"), enabled(YearMonth.of(2026, 7)))
        assertEquals(listOf("Last 2 months", "Last 3 months"), enabled(YearMonth.of(2026, 5)))
        assertEquals(listOf("Last 2 months", "Last 3 months", "Last 6 months"), enabled(YearMonth.of(2026, 4)))
    }
}
