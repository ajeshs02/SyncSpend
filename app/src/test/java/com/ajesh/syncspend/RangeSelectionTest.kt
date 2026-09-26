package com.ajesh.syncspend

import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.domain.model.RangeSelection
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RangeSelectionTest {
    private val today = LocalDate.of(2026, 9, 26)
    private fun d(m: Int, day: Int) = LocalDate.of(2026, m, day)

    @Test fun firstTapStartsThenSecondTapCompletes() {
        val a = RangeSelection().tapDay(d(9, 10))
        assertEquals(d(9, 10), a.start)
        assertNull(a.end)
        assertFalse(a.isComplete)
        val b = a.tapDay(d(9, 20))
        assertTrue(b.isComplete)
        assertEquals(DateRange(d(9, 10), d(9, 20)), b.range)
    }

    @Test fun tappingTheStartAgainMakesASingleDay() {
        val sel = RangeSelection().tapDay(d(9, 10)).tapDay(d(9, 10))
        assertEquals(DateRange(d(9, 10), d(9, 10)), sel.range)
        assertEquals(1, sel.range!!.dayCount)
    }

    @Test fun tappingBeforeTheStartMovesTheStart() {
        val sel = RangeSelection().tapDay(d(9, 10)).tapDay(d(9, 3))
        assertEquals(d(9, 3), sel.start)
        assertNull(sel.end)
    }

    @Test fun anyTapOnACompleteRangeStartsOver() {
        val full = RangeSelection().tapDay(d(9, 10)).tapDay(d(9, 20))
        val inside = full.tapDay(d(9, 15))
        assertEquals(d(9, 15), inside.start)
        assertNull(inside.end)
        val outside = full.tapDay(d(8, 1))
        assertEquals(d(8, 1), outside.start)
        assertNull(outside.end)
    }

    @Test fun monthTitlesSelectWholeMonthsAndFillBetween() {
        val first = RangeSelection().tapMonth(YearMonth.of(2026, 7), today)
        assertEquals(DateRange(d(7, 1), d(7, 31)), first.range)
        val filled = first.tapMonth(YearMonth.of(2026, 9), today)
        // September is the current month, so it stops at today rather than the 30th.
        assertEquals(DateRange(d(7, 1), d(9, 26)), filled.range)
        // Order of the two taps doesn't matter.
        val reversed = RangeSelection().tapMonth(YearMonth.of(2026, 9), today).tapMonth(YearMonth.of(2026, 7), today)
        assertEquals(filled.range, reversed.range)
    }

    @Test fun thirdMonthTapStartsOver() {
        val done = RangeSelection().tapMonth(YearMonth.of(2026, 7), today).tapMonth(YearMonth.of(2026, 8), today)
        assertEquals(DateRange(d(7, 1), d(8, 31)), done.range)
        val again = done.tapMonth(YearMonth.of(2026, 6), today)
        assertEquals(DateRange(d(6, 1), d(6, 30)), again.range)
    }

    @Test fun dayTapAfterMonthSelectionStartsAFreshRange() {
        val month = RangeSelection().tapMonth(YearMonth.of(2026, 8), today)
        val day = month.tapDay(d(8, 12))
        assertEquals(d(8, 12), day.start)
        assertNull(day.end)
    }
}
