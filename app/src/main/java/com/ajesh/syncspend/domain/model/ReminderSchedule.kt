package com.ajesh.syncspend.domain.model

/**
 * Repeat rule for a custom reminder. WEEKLY repeats on the weekday of the
 * start date; MONTHLY repeats on its day-of-month (the design's "28th monthly").
 */
enum class ReminderSchedule { ONCE, DAILY, WEEKLY, MONTHLY }
