package com.ajesh.syncspend.util

import com.ajesh.syncspend.domain.model.DateRange
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    /** "14 September 2026" — the design's `longDate`. */
    fun longDate(d: LocalDate): String =
        "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.FULL, Locale.US)} ${d.year}"

    /** "14 Sep" — used on the keypad's date key and row day labels. */
    fun shortDate(d: LocalDate): String =
        "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, Locale.US)}"

    /** "14:35" (24-hour) from minute-of-day. */
    fun hhmm(minuteOfDay: Int): String {
        val h = minuteOfDay / 60
        val m = minuteOfDay % 60
        return "${if (h < 10) "0" else ""}$h:${if (m < 10) "0" else ""}$m"
    }

    /** A list row's date line: "27 Sep", or "27 Sep - 14:35" when the entry's time is known. */
    fun entryDayLabel(date: LocalDate, timeMinuteOfDay: Int?): String =
        if (timeMinuteOfDay == null) shortDate(date) else "${shortDate(date)} - ${hhmm(timeMinuteOfDay)}"

    /** "14 Sep 2026". */
    fun shortDateYear(d: LocalDate): String = "${shortDate(d)} ${d.year}"

    /** [shortDate], with the year appended only once [d] falls outside [today]'s year. */
    fun smartDate(d: LocalDate, today: LocalDate): String = if (d.year == today.year) shortDate(d) else shortDateYear(d)

    /**
     * Compact label for a custom range: "12 Aug - 3 Sep" within [today]'s year,
     * with years added when the range reaches into another year.
     */
    fun rangeLabel(start: LocalDate, end: LocalDate, today: LocalDate = LocalDate.now()): String {
        if (start == end) return if (start.year == today.year) shortDate(start) else shortDateYear(start)
        return if (start.year == today.year && end.year == today.year) {
            "${shortDate(start)} - ${shortDate(end)}"
        } else {
            "${shortDateYear(start)} - ${shortDateYear(end)}"
        }
    }

    /**
     * The literal applied-range subheading Stats/Transactions show below their period picker:
     * "From 1 Oct 2026 - To 31 Oct 2026". Always both full dates with the year, regardless of whether
     * the range falls in the current year — this is a precise "here's exactly what was calculated"
     * statement, not a compact label, so it's never abbreviated the way [rangeLabel] is.
     */
    fun appliedRangeLabel(range: DateRange): String = "From ${shortDateYear(range.start)} - To ${shortDateYear(range.end)}"

    /** "Sep 2026" — a whole month, e.g. a Forecast row's month chip. */
    fun monthYearLabel(d: LocalDate): String = "${d.month.getDisplayName(TextStyle.SHORT, Locale.US)} ${d.year}"

    /** "Jul - Sep 2026", or with both years when the span crosses one ("Nov 2025 - Jan 2026"). */
    fun monthSpanLabel(first: YearMonth, last: YearMonth): String {
        fun short(m: YearMonth) = m.month.getDisplayName(TextStyle.SHORT, Locale.US)
        return if (first.year == last.year) "${short(first)} - ${short(last)} ${last.year}"
        else "${short(first)} ${first.year} - ${short(last)} ${last.year}"
    }

    /** "12:05 AM"-style label from minute-of-day, matching the design's `fmt12`. */
    fun fmt12(minuteOfDay: Int): String {
        val h = minuteOfDay / 60
        val m = minuteOfDay % 60
        val ap = if (h >= 12) "PM" else "AM"
        val hh = if (h % 12 == 0) 12 else h % 12
        return "$hh:${m.toString().padStart(2, '0')} $ap"
    }
}
