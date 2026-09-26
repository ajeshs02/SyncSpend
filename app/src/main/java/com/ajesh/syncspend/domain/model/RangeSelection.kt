package com.ajesh.syncspend.domain.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * The in-progress selection of the from/to calendar, "hotel booking" style.
 *
 * Day taps: the first tap sets the start (awaiting an end); the next tap on or
 * after it sets the end; a tap before it moves the start; and once a full range
 * exists any tap begins a fresh selection.
 *
 * Month-title taps: the first selects that whole month and remembers it
 * ([pendingMonth]); tapping another month title then fills every whole month
 * in between; the next tap starts over. Both styles share one state, so the
 * result is always simply a [start]..[end] span.
 */
data class RangeSelection(
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val pendingMonth: YearMonth? = null,
) {
    val isComplete: Boolean get() = start != null && end != null
    val range: DateRange? get() = start?.let { DateRange(it, end ?: it) }

    fun tapDay(day: LocalDate): RangeSelection = when {
        start == null || end != null -> RangeSelection(start = day)
        day.isBefore(start) -> RangeSelection(start = day)
        else -> RangeSelection(start = start, end = day)
    }

    /** [today] caps a month that hasn't finished yet — you can't select days that haven't happened. */
    fun tapMonth(month: YearMonth, today: LocalDate): RangeSelection {
        fun last(m: YearMonth) = minOf(m.atEndOfMonth(), today)
        val anchor = pendingMonth
        return if (anchor == null) {
            RangeSelection(month.atDay(1), last(month), pendingMonth = month)
        } else {
            val from = minOf(anchor, month)
            val to = maxOf(anchor, month)
            RangeSelection(from.atDay(1), last(to))
        }
    }
}
