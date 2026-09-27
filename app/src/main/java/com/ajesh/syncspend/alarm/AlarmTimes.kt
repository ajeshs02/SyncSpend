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

    /**
     * One alert of a subscription: it goes off at [atMillis], [daysBefore] days ahead of the due day
     * [dueDate] (0 = on the day itself).
     */
    data class SubscriptionAlert(val atMillis: Long, val daysBefore: Int, val dueDate: LocalDate)

    /**
     * The next alert strictly after [now]: on the due day at 9:00, and on each of [offsets] days before
     * it. Due days already fully in the past are rolled forward by the billing cycle.
     */
    fun nextSubscriptionAlert(
        due: LocalDate,
        cycle: BillingCycle,
        offsets: Collection<Int>,
        now: Long = System.currentTimeMillis(),
    ): SubscriptionAlert {
        val daysBefore = (offsets.filter { it > 0 } + 0).distinct().sortedDescending() // earliest alert of a due day first
        var date = due
        var guard = 0
        while (guard++ < 2000) {
            for (d in daysBefore) {
                val at = toMillis(date.minusDays(d.toLong()), SUBSCRIPTION_MINUTE_OF_DAY)
                if (at > now) return SubscriptionAlert(at, d, date)
            }
            date = advance(date, cycle)
        }
        return SubscriptionAlert(toMillis(date, SUBSCRIPTION_MINUTE_OF_DAY), 0, date)
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

    /** The due date to show once a paused subscription is switched back on: the first one after [now]. */
    fun nextSubscriptionDate(due: LocalDate, cycle: BillingCycle, now: Long = System.currentTimeMillis()): LocalDate {
        var date = due
        var guard = 0
        while (toMillis(date, SUBSCRIPTION_MINUTE_OF_DAY) <= now && guard++ < 2000) date = advance(date, cycle)
        return date
    }

    /** Same for a reminder; null for a ONCE reminder whose moment has passed (it needs a new date). */
    fun nextReminderDate(
        start: LocalDate,
        schedule: ReminderSchedule,
        minuteOfDay: Int,
        now: Long = System.currentTimeMillis(),
    ): LocalDate? {
        var date = start
        if (schedule == ReminderSchedule.ONCE) return if (toMillis(date, minuteOfDay) > now) date else null
        var guard = 0
        while (toMillis(date, minuteOfDay) <= now && guard++ < 5000) date = advance(date, schedule)
        return date
    }
}
