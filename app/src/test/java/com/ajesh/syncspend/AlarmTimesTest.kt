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

    private val nineAm = AlarmTimes.SUBSCRIPTION_MINUTE_OF_DAY
    private fun alert(due: LocalDate, offsets: List<Int>, at: Long = now) = AlarmTimes.nextSubscriptionAlert(due, BillingCycle.MONTHLY, offsets, now = at)

    @Test fun subscriptionRollsForwardPastDueDates() {
        val alert = alert(LocalDate.of(2026, 6, 22), emptyList())
        assertEquals(AlarmTimes.toMillis(LocalDate.of(2026, 9, 22), nineAm), alert.atMillis)
        assertEquals(0, alert.daysBefore)
        assertEquals(LocalDate.of(2026, 9, 22), alert.dueDate)
    }

    @Test fun theDayItselfAlwaysAlertsAndEarlierOnesComeFirst() {
        // now = Sep 14 noon, due Sep 22: 3 days before = Sep 19 9:00 comes first, then Sep 21, then the day.
        val due = LocalDate.of(2026, 9, 22)
        val first = alert(due, listOf(1, 3))
        assertEquals(3, first.daysBefore)
        assertEquals(AlarmTimes.toMillis(LocalDate.of(2026, 9, 19), nineAm), first.atMillis)
        val second = alert(due, listOf(1, 3), first.atMillis)
        assertEquals(1, second.daysBefore)
        assertEquals(AlarmTimes.toMillis(LocalDate.of(2026, 9, 21), nineAm), second.atMillis)
        val third = alert(due, listOf(1, 3), second.atMillis)
        assertEquals(0, third.daysBefore)
        assertEquals(AlarmTimes.toMillis(due, nineAm), third.atMillis)
        // After the day's alert the next due day (a month on) starts over with its earliest alert.
        val next = alert(due, listOf(1, 3), third.atMillis)
        assertEquals(LocalDate.of(2026, 10, 22), next.dueDate)
        assertEquals(3, next.daysBefore)
    }

    @Test fun anAlertAlreadyInThePastIsSkippedNotFiredLate() {
        // due Sep 15 with 3 and 1 days before: both earlier alerts (Sep 12 and 14 at 9:00) are past at Sep 14 noon.
        val out = alert(LocalDate.of(2026, 9, 15), listOf(1, 3))
        assertEquals(0, out.daysBefore)
        assertEquals(AlarmTimes.toMillis(LocalDate.of(2026, 9, 15), nineAm), out.atMillis)
        // due Sep 16: the 1-day alert (Sep 15) is still ahead
        assertEquals(1, alert(LocalDate.of(2026, 9, 16), listOf(1, 3)).daysBefore)
    }

    @Test fun offsetsAreCleanedUp() {
        val out = alert(LocalDate.of(2026, 9, 22), listOf(3, 3, -2, 0, 1))
        assertEquals(3, out.daysBefore)
        assertEquals(listOf(1, 3), com.ajesh.syncspend.domain.model.RemindOffsets.parse("3, 1,x,0,-4,3"))
        assertEquals("1,3,7", com.ajesh.syncspend.domain.model.RemindOffsets.format(listOf(7, 1, 3, 3)))
        assertEquals(com.ajesh.syncspend.domain.model.RemindOffsets.DEFAULT, com.ajesh.syncspend.domain.model.RemindOffsets.parse(com.ajesh.syncspend.domain.model.RemindOffsets.DEFAULT_TEXT))
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
            AlarmTimes.nextSubscriptionDate(LocalDate.of(2026, 6, 22), BillingCycle.MONTHLY, now = now),
        )
        // Already in the future: unchanged.
        assertEquals(
            LocalDate.of(2026, 10, 1),
            AlarmTimes.nextSubscriptionDate(LocalDate.of(2026, 10, 1), BillingCycle.MONTHLY, now = now),
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
