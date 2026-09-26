package com.ajesh.syncspend.util

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    /** "14 September 2026" — the design's `longDate`. */
    fun longDate(d: LocalDate): String =
        "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.FULL, Locale.US)} ${d.year}"

    /** "14 Sep" — used on the keypad's date key and row day labels. */
    fun shortDate(d: LocalDate): String =
        "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, Locale.US)}"

    /** "12:05 AM"-style label from minute-of-day, matching the design's `fmt12`. */
    fun fmt12(minuteOfDay: Int): String {
        val h = minuteOfDay / 60
        val m = minuteOfDay % 60
        val ap = if (h >= 12) "PM" else "AM"
        val hh = if (h % 12 == 0) 12 else h % 12
        return "$hh:${m.toString().padStart(2, '0')} $ap"
    }
}
