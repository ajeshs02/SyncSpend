package com.ajesh.syncspend

import com.ajesh.syncspend.alarm.AlarmTimes
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.ReminderSchedule
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmTimesTest {
    private val now = AlarmTimes.toMillis(LocalDate.of(2026, 9, 14), 12 * 60) // noon

    @Test fun dailyIsAlwaysInTheNext24Hours() {
        listOf(0, 9 * 60, 23 * 60 + 59).forEach { minute ->
            val next = AlarmTimes.nextDaily(minute)
            assertTrue(next > System.currentTimeMillis())
            assertTrue(next - System.currentTimeMillis() <= 24 * 3600 * 1000L + 1000)
        }
    }

    @Test fun subscriptionRollsForwardPastDueDates() {
        val at = AlarmTimes.nextSubscription(LocalDate.of(2026, 6, 22), BillingCycle.MONTHLY, now)
        assertEquals(AlarmTimes.toMillis(LocalDate.of(2026, 9, 22), AlarmTimes.SUBSCRIPTION_MINUTE_OF_DAY), at)
    }

    @Test fun onceRemindersInThePastAreNotScheduled() {
        assertNull(AlarmTimes.nextReminder(LocalDate.of(2026, 9, 1), ReminderSchedule.ONCE, 9 * 60, now))
        assertNotNull(AlarmTimes.nextReminder(LocalDate.of(2026, 9, 30), ReminderSchedule.ONCE, 9 * 60, now))
    }

    @Test fun monthlyAndWeeklyReminders() {
        assertEquals(
            AlarmTimes.toMillis(LocalDate.of(2026, 9, 28), 9 * 60),
            AlarmTimes.nextReminder(LocalDate.of(2026, 7, 28), ReminderSchedule.MONTHLY, 9 * 60, now),
        )
        assertEquals(
            AlarmTimes.toMillis(LocalDate.of(2026, 9, 21), 9 * 60),
            AlarmTimes.nextReminder(LocalDate.of(2026, 9, 7), ReminderSchedule.WEEKLY, 9 * 60, now),
        )
    }

    @Test fun resumedSubscriptionShowsItsNextFutureDueDate() {
        assertEquals(
            LocalDate.of(2026, 9, 22),
            AlarmTimes.nextSubscriptionDate(LocalDate.of(2026, 6, 22), BillingCycle.MONTHLY, now),
        )
        // Already in the future: unchanged.
        assertEquals(
            LocalDate.of(2026, 10, 1),
            AlarmTimes.nextSubscriptionDate(LocalDate.of(2026, 10, 1), BillingCycle.MONTHLY, now),
        )
    }

    @Test fun resumedReminderRollsForwardOrAsksForANewDate() {
        assertEquals(
            LocalDate.of(2026, 9, 15),
            AlarmTimes.nextReminderDate(LocalDate.of(2026, 9, 1), ReminderSchedule.DAILY, 9 * 60, now),
        )
        assertEquals(
            LocalDate.of(2026, 9, 14),
            AlarmTimes.nextReminderDate(LocalDate.of(2026, 9, 14), ReminderSchedule.DAILY, 20 * 60, now),
        )
        assertNull(AlarmTimes.nextReminderDate(LocalDate.of(2026, 9, 1), ReminderSchedule.ONCE, 9 * 60, now))
        assertEquals(
            LocalDate.of(2026, 9, 20),
            AlarmTimes.nextReminderDate(LocalDate.of(2026, 9, 20), ReminderSchedule.ONCE, 9 * 60, now),
        )
    }
}
