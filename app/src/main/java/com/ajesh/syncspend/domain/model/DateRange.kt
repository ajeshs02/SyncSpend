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
}
