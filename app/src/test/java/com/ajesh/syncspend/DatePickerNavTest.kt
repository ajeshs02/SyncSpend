package com.ajesh.syncspend

import com.ajesh.syncspend.ui.components.DatePickerNav
import java.time.YearMonth
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The round-9 bug: the day-level calendar's year Prev/Next arrows followed month-granularity
 * availability instead of their own. These confirm the two are now independently gated, in both
 * directions, including the case where they correctly agree.
 */
class DatePickerNavTest {
    @Test fun yearPrevIsDisabledWhileMonthPrevIsStillEnabledWithinTheSameYear() {
        val viewing = YearMonth.of(2026, 7)
        val lower = YearMonth.of(2026, 3)
        assertFalse("no earlier year to jump to", DatePickerNav.yearPrevEnabled(viewing, lower))
        assertTrue("an earlier month in the same year is still reachable", DatePickerNav.monthPrevEnabled(viewing, lower))
    }

    @Test fun yearNextIsDisabledWhileMonthNextIsStillEnabledWithinTheSameYear() {
        val viewing = YearMonth.of(2026, 7)
        val upper = YearMonth.of(2026, 12)
        assertFalse("no later year to jump to", DatePickerNav.yearNextEnabled(viewing, upper))
        assertTrue("a later month in the same year is still reachable", DatePickerNav.monthNextEnabled(viewing, upper))
    }

    @Test fun bothGranularitiesAgreeOnceTheBoundIsAnEarlierYear() {
        val viewing = YearMonth.of(2026, 1)
        val lower = YearMonth.of(2025, 3)
        assertTrue(DatePickerNav.yearPrevEnabled(viewing, lower))
        assertTrue(DatePickerNav.monthPrevEnabled(viewing, lower))
    }

    @Test fun bothGranularitiesAgreeOnceTheBoundIsALaterYear() {
        val viewing = YearMonth.of(2026, 12)
        val upper = YearMonth.of(2027, 6)
        assertTrue(DatePickerNav.yearNextEnabled(viewing, upper))
        assertTrue(DatePickerNav.monthNextEnabled(viewing, upper))
    }

    @Test fun bothGranularitiesAgreeAtTheExactFloorAndCeiling() {
        val at = YearMonth.of(2026, 3)
        assertFalse(DatePickerNav.yearPrevEnabled(at, at))
        assertFalse(DatePickerNav.monthPrevEnabled(at, at))
        assertFalse(DatePickerNav.yearNextEnabled(at, at))
        assertFalse(DatePickerNav.monthNextEnabled(at, at))
    }
}
