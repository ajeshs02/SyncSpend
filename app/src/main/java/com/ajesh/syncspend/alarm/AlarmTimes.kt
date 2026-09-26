package com.ajesh.syncspend.alarm

import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.ReminderSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Pure date math for alarms, kept separate from Android APIs so it stays easy to reason about. */
object AlarmTimes {
    /** Subscriptions notify at 9:00 AM on the due date. */
    const val SUBSCRIPTION_MINUTE_OF_DAY = 9 * 60

    fun toMillis(date: LocalDate, minuteOfDay: Int): Long =
        LocalDateTime.of(date, LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun advance(date: LocalDate, cycle: BillingCycle): LocalDate = when (cycle) {
        BillingCycle.WEEKLY -> date.plusWeeks(1)
        BillingCycle.MONTHLY -> date.plusMonths(1)
        BillingCycle.YEARLY -> date.plusYears(1)
    }

    fun advance(date: LocalDate, schedule: ReminderSchedule): LocalDate = when (schedule) {
        ReminderSchedule.ONCE -> date
        ReminderSchedule.DAILY -> date.plusDays(1)
        ReminderSchedule.WEEKLY -> date.plusWeeks(1)
        ReminderSchedule.MONTHLY -> date.plusMonths(1)
    }

    /** Next daily-reminder instant strictly after [now]. */
    fun nextDaily(minuteOfDay: Int, now: Long = System.currentTimeMillis()): Long {
        var date = LocalDate.now()
        var millis = toMillis(date, minuteOfDay)
        if (millis <= now) {
            date = date.plusDays(1)
            millis = toMillis(date, minuteOfDay)
        }
        return millis
    }

    /** First recurrence of a subscription strictly after [now], or null if it can't be computed. */
    fun nextSubscription(due: LocalDate, cycle: BillingCycle, now: Long = System.currentTimeMillis()): Long {
        var date = due
        var millis = toMillis(date, SUBSCRIPTION_MINUTE_OF_DAY)
        var guard = 0
        while (millis <= now && guard++ < 2000) {
            date = advance(date, cycle)
            millis = toMillis(date, SUBSCRIPTION_MINUTE_OF_DAY)
        }
        return millis
    }

    /** First recurrence of a reminder strictly after [now]; null for a ONCE reminder already in the past. */
    fun nextReminder(
        start: LocalDate,
        schedule: ReminderSchedule,
        minuteOfDay: Int,
        now: Long = System.currentTimeMillis(),
    ): Long? {
        var date = start
        var millis = toMillis(date, minuteOfDay)
        if (schedule == ReminderSchedule.ONCE) return if (millis > now) millis else null
        var guard = 0
        while (millis <= now && guard++ < 5000) {
            date = advance(date, schedule)
            millis = toMillis(date, minuteOfDay)
        }
        return millis
    }
}
