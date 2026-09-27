package com.ajesh.syncspend.domain.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * The period the Stats tab analyses. Windows are calendar-aligned (whole months
 * or a whole year) and each has an equally long window immediately before it to
 * compare against — except [ALL_TIME], which has nothing to compare with.
 */
enum class StatsRange(val label: String, private val months: Int) {
    THIS_MONTH("This month", 1),
    LAST_MONTH("Last month", 1),
    LAST_3_MONTHS("3 months", 3),
    LAST_6_MONTHS("6 months", 6),
    THIS_YEAR("This year", 12),
    ALL_TIME("All time", 0);

    fun resolve(today: LocalDate, earliest: LocalDate?): DateRange {
        val current = YearMonth.from(today)
        return when (this) {
            THIS_MONTH -> window(current, 1)
            LAST_MONTH -> window(current.minusMonths(1), 1)
            LAST_3_MONTHS -> window(current.minusMonths(2), 3)
            LAST_6_MONTHS -> window(current.minusMonths(5), 6)
            THIS_YEAR -> DateRange(LocalDate.of(today.year, 1, 1), LocalDate.of(today.year, 12, 31))
            ALL_TIME -> DateRange(minOf(earliest ?: today, today), today)
        }
    }

    /** The window of the same length right before [current]; null for [ALL_TIME]. */
    fun previous(current: DateRange): DateRange? = when (this) {
        ALL_TIME -> null
        else -> DateRange(
            current.start.minusMonths(months.toLong()),
            YearMonth.from(current.end).minusMonths(months.toLong()).atEndOfMonth(),
        )
    }

    /** "September 2026", "Jul - Sep 2026", "2026" or "All time". */
    fun describe(range: DateRange): String {
        fun short(m: YearMonth) = m.month.getDisplayName(TextStyle.SHORT, Locale.US)
        val first = YearMonth.from(range.start)
        val last = YearMonth.from(range.end)
        return when (this) {
            THIS_MONTH, LAST_MONTH -> "${first.month.getDisplayName(TextStyle.FULL, Locale.US)} ${first.year}"
            LAST_3_MONTHS, LAST_6_MONTHS ->
                if (first.year == last.year) "${short(first)} - ${short(last)} ${last.year}"
                else "${short(first)} ${first.year} - ${short(last)} ${last.year}"
            THIS_YEAR -> first.year.toString()
            ALL_TIME -> "All time"
        }
    }

    private fun window(start: YearMonth, count: Int) = DateRange(start.atDay(1), start.plusMonths(count - 1L).atEndOfMonth())
}
