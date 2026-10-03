package com.ajesh.syncspend.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** An inclusive from/to date span (the Transactions "Custom" filter). */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    init {
        require(!end.isBefore(start)) { "end must not be before start" }
    }

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    val dayCount: Long get() = ChronoUnit.DAYS.between(start, end) + 1

    companion object {
        /** Jan 1 to Dec 31 of [today]'s year — always derived, never a hardcoded year (same rule [ScopePeriod.Year] follows). */
        fun currentYear(today: LocalDate): DateRange = DateRange(LocalDate.of(today.year, 1, 1), LocalDate.of(today.year, 12, 31))

        /** Jan 1 to Dec 31 of the year before [today]'s — same derivation rule as [currentYear], one year back. */
        fun lastYear(today: LocalDate): DateRange = DateRange(LocalDate.of(today.year - 1, 1, 1), LocalDate.of(today.year - 1, 12, 31))

        /** The earliest known entry (or [today], if there isn't one yet) through [today]. */
        fun allTime(earliest: LocalDate?, today: LocalDate): DateRange = DateRange(minOf(earliest ?: today, today), today)
    }
}
