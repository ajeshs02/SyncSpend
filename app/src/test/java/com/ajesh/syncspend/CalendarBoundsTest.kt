package com.ajesh.syncspend

import com.ajesh.syncspend.domain.model.CalendarBounds
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.StatsRange
import com.ajesh.syncspend.domain.model.TransactionsTab
import com.ajesh.syncspend.ui.transactions.headerRangeLabel
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarBoundsTest {
    @Test fun addEntryCalendarStartsAtTheFirstOfTheMonthTwoBack() {
        // "If today is Sep 27 the range begins Jul 1; if today is Oct 3 it begins Aug 1."
        assertEquals(YearMonth.of(2026, 7), CalendarBounds.entryLowerMonth(YearMonth.of(2026, 9), null))
        assertEquals(YearMonth.of(2026, 8), CalendarBounds.entryLowerMonth(YearMonth.of(2026, 10), null))
        assertEquals(YearMonth.of(2025, 11), CalendarBounds.entryLowerMonth(YearMonth.of(2026, 1), null)) // across the year
    }

    @Test fun addEntryCalendarReachesAnOlderEntryButNeverLessThanTheBuffer() {
        val now = YearMonth.of(2026, 9)
        assertEquals(YearMonth.of(2025, 3), CalendarBounds.entryLowerMonth(now, LocalDate.of(2025, 3, 9)))
        assertEquals(YearMonth.of(2026, 7), CalendarBounds.entryLowerMonth(now, LocalDate.of(2026, 9, 2))) // buffer wins over a recent first entry
    }

    @Test fun everyOtherCalendarOnlyGoesBackToTheOldestEntry() {
        val now = YearMonth.of(2026, 9)
        assertEquals(now, CalendarBounds.dataLowerMonth(now, null)) // no entries: just this month, no buffer
        assertEquals(YearMonth.of(2026, 8), CalendarBounds.dataLowerMonth(now, LocalDate.of(2026, 8, 20)))
        assertEquals(YearMonth.of(2024, 2), CalendarBounds.dataLowerMonth(now, LocalDate.of(2024, 2, 1)))
        assertEquals(now, CalendarBounds.dataLowerMonth(now, LocalDate.of(2026, 11, 5))) // a future-dated first entry
    }

    @Test fun aWindowNeedsTheEntriesToReachItsFirstMonth() {
        val now = YearMonth.of(2026, 9)
        assertFalse(CalendarBounds.windowEnabled(3, now, YearMonth.of(2026, 8)))
        assertTrue(CalendarBounds.windowEnabled(3, now, YearMonth.of(2026, 7)))
        assertTrue(CalendarBounds.windowEnabled(3, now, YearMonth.of(2025, 1)))
        assertFalse(CalendarBounds.windowEnabled(6, now, YearMonth.of(2026, 5)))
        assertTrue(CalendarBounds.windowEnabled(6, now, YearMonth.of(2026, 4)))
        assertFalse(CalendarBounds.windowEnabled(3, now, now)) // a brand-new install
    }

    @Test fun theHeaderShowsTheDatesTheCurrentChipCovers() {
        val thursday = LocalDate.of(2026, 9, 17)
        val entries = TransactionsTab.ENTRIES
        assertEquals("14 Sep - 17 Sep", headerRangeLabel(entries, EntryFilter.THIS_WEEK, null, StatsRange.THIS_MONTH, thursday))
        assertEquals("1 Sep - 17 Sep", headerRangeLabel(entries, EntryFilter.THIS_MONTH, null, StatsRange.THIS_MONTH, thursday))
        assertEquals("1 Aug - 31 Aug", headerRangeLabel(TransactionsTab.CATEGORIES, EntryFilter.LAST_MONTH, null, StatsRange.THIS_MONTH, thursday))
        assertEquals("2 Jun - 9 Jun", headerRangeLabel(entries, EntryFilter.CUSTOM, DateRange(LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 9)), StatsRange.THIS_MONTH, thursday))
        assertNull(headerRangeLabel(entries, EntryFilter.CUSTOM, null, StatsRange.THIS_MONTH, thursday))
    }

    @Test fun theStatsTabShowsItsOwnRangeUpToToday() {
        val thursday = LocalDate.of(2026, 9, 17)
        val stats = TransactionsTab.ANALYTICS
        assertEquals("1 Sep - 17 Sep", headerRangeLabel(stats, EntryFilter.THIS_WEEK, null, StatsRange.THIS_MONTH, thursday))
        assertEquals("1 Aug - 31 Aug", headerRangeLabel(stats, EntryFilter.THIS_WEEK, null, StatsRange.LAST_MONTH, thursday))
        assertEquals("1 Jul - 17 Sep", headerRangeLabel(stats, EntryFilter.THIS_WEEK, null, StatsRange.LAST_3_MONTHS, thursday))
        assertEquals("All time", headerRangeLabel(stats, EntryFilter.THIS_WEEK, null, StatsRange.ALL_TIME, thursday))
    }
}
