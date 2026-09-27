package com.ajesh.syncspend

import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.ScopePeriod
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScopePeriodTest {
    private val sep = YearMonth.of(2026, 9)
    private var id = 1L
    private fun tx(date: LocalDate) = TransactionEntity(id++, -10.0, "x", 1, date, id)
    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)

    @Test fun lastMonthsIsWholeCalendarMonthsEndingWithEndMonth() {
        val all = listOf(tx(d(6, 30)), tx(d(7, 1)), tx(d(8, 15)), tx(d(9, 30)), tx(d(10, 1)))
        val picked = AnalyticsEngine.scopeFilter(all, ScopePeriod.LastMonths(3, sep)).map { it.date }
        assertEquals(listOf(d(7, 1), d(8, 15), d(9, 30)), picked)
    }

    @Test fun lastMonthsWindowsCrossYearBoundaries() {
        val scope = ScopePeriod.LastMonths(6, YearMonth.of(2026, 2))
        assertEquals(YearMonth.of(2025, 9), scope.startMonth)
        val all = listOf(tx(d(8, 31, 2025)), tx(d(9, 1, 2025)), tx(d(12, 31, 2025)), tx(d(2, 28)), tx(d(3, 1)))
        assertEquals(3, AnalyticsEngine.scopeFilter(all, scope).size)
    }

    @Test fun previousWindowIsTheNMonthsBefore() {
        val prev = AnalyticsEngine.previousScope(ScopePeriod.LastMonths(3, sep)) as ScopePeriod.LastMonths
        assertEquals(YearMonth.of(2026, 6), prev.endMonth)
        assertEquals(YearMonth.of(2026, 4), prev.startMonth)
    }

    @Test fun labelsNameTheWindowWhileCurrentAndItsSpanOtherwise() {
        assertEquals("Last 3 months", AnalyticsEngine.scopeLabel(ScopePeriod.LastMonths(3, sep), now = sep))
        assertEquals("Apr - Jun 2026", AnalyticsEngine.scopeLabel(ScopePeriod.LastMonths(3, YearMonth.of(2026, 6)), now = sep))
        assertEquals("Sep 2025 - Feb 2026", AnalyticsEngine.scopeLabel(ScopePeriod.LastMonths(6, YearMonth.of(2026, 2)), now = sep))
        assertEquals("September 2026", AnalyticsEngine.scopeLabel(ScopePeriod.Month(sep), now = sep))
        assertEquals("2026", AnalyticsEngine.scopeLabel(ScopePeriod.Year(2026), now = sep))
    }

    private val lower = YearMonth.of(2026, 7) // two months back from September

    @Test fun monthSteppingStopsAtThePresentAndAtTheLowerBound() {
        assertEquals(ScopePeriod.Month(YearMonth.of(2026, 8)), AnalyticsEngine.stepScope(ScopePeriod.Month(sep), -1, sep, lower))
        assertNull(AnalyticsEngine.stepScope(ScopePeriod.Month(sep), 1, sep, lower))
        assertNull(AnalyticsEngine.stepScope(ScopePeriod.Month(lower), -1, sep, lower))
    }

    @Test fun yearSteppingStaysWithinTheBrowsableYears() {
        assertNull(AnalyticsEngine.stepScope(ScopePeriod.Year(2026), 1, sep, lower))
        assertNull(AnalyticsEngine.stepScope(ScopePeriod.Year(2026), -1, sep, lower))
        assertEquals(ScopePeriod.Year(2025), AnalyticsEngine.stepScope(ScopePeriod.Year(2026), -1, sep, YearMonth.of(2025, 3)))
    }

    @Test fun lastMonthsSteppingMovesTheEndOfTheWindow() {
        val threeMonths = ScopePeriod.LastMonths(3, sep) // Jul..Sep, start == lower
        assertNull(AnalyticsEngine.stepScope(threeMonths, 1, sep, lower)) // already at the present
        assertNull(AnalyticsEngine.stepScope(threeMonths, -1, sep, lower)) // would start before the lower bound
        val older = YearMonth.of(2026, 1)
        assertEquals(ScopePeriod.LastMonths(3, YearMonth.of(2026, 8)), AnalyticsEngine.stepScope(threeMonths, -1, sep, older))
        assertNull(AnalyticsEngine.stepScope(ScopePeriod.AllTime, 1, sep, lower))
    }
}
