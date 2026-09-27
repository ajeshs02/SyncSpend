package com.ajesh.syncspend.domain.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * How far back each calendar lets you go. Only the calendar for adding or editing an entry keeps a
 * three-month buffer, so a fresh install can still log something for last month; every other
 * calendar and range picker only goes back as far as the oldest entry actually is.
 */
object CalendarBounds {
    /**
     * Add / Edit Entry: the first day of the month two back, so three calendar months including this
     * one (today Sep 27 gives Jul 1; Oct 3 gives Aug 1) — or the oldest entry's month when that is older.
     */
    fun entryLowerMonth(now: YearMonth, earliest: LocalDate?): YearMonth =
        minOf(now.minusMonths(2), earliest?.let { YearMonth.from(it) } ?: now)

    /**
     * Home's period picker and arrows, Custom range and the CSV export picker: the oldest entry's
     * month, or just this month while there are no entries.
     */
    fun dataLowerMonth(now: YearMonth, earliest: LocalDate?): YearMonth {
        val oldest = earliest?.let { YearMonth.from(it) } ?: return now
        return if (oldest.isAfter(now)) now else oldest
    }

    /**
     * A "Last N months" window (this month and the N-1 before it) is offered once the oldest entry
     * reaches into its first month, i.e. [lowerBound] (from [dataLowerMonth]) is no later than that month.
     */
    fun windowEnabled(months: Int, now: YearMonth, lowerBound: YearMonth): Boolean =
        !lowerBound.isAfter(now.minusMonths(months - 1L))
}
