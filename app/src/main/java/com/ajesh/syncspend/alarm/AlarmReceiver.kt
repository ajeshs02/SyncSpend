package com.ajesh.syncspend.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ajesh.syncspend.SyncSpendApp
import com.ajesh.syncspend.domain.model.AppLink
import com.ajesh.syncspend.domain.model.ReminderSchedule
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires the notification for a due alarm, then re-arms the next occurrence.
 * DB/DataStore work runs off the main thread under goAsync() so the receiver
 * can't ANR the broadcast dispatch.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE) ?: return
        val id = intent.getLongExtra(EXTRA_ID, 0)
        val daysBefore = intent.getIntExtra(EXTRA_DAYS_BEFORE, 0)
        val dueDay = intent.getLongExtra(EXTRA_DUE_DAY, NO_DUE_DAY)
        val app = context.applicationContext as SyncSpendApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (type) {
                    TYPE_DAILY -> fireDaily(app)
                    TYPE_SUBSCRIPTION -> fireSubscription(app, id, daysBefore, dueDay)
                    TYPE_REMINDER -> fireReminder(app, id)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun fireDaily(app: SyncSpendApp) {
        val c = app.container
        val prefs = c.preferencesRepository.preferences.first()
        if (!prefs.dailyReminderEnabled) return
        // "Add" opens Add Entry (and clears this notification); "Done" just clears it.
        NotificationHelper.show(
            app, NOTIF_DAILY, "Log today's spend",
            "Take a moment to record what you spent or earned today.",
            actions = dailyActions(app),
        )
        c.alarmScheduler.scheduleDailyReminder(prefs.dailyReminderMinuteOfDay)
    }

    /**
     * [daysBefore] > 0 is an early heads-up (a "remind me N days before" alert); 0 is the due day itself, the
     * only alert that moves the due date on to the next period. Tapping opens Subscriptions with this one on top.
     */
    private suspend fun fireSubscription(app: SyncSpendApp, id: Long, daysBefore: Int, dueDay: Long) {
        val c = app.container
        val sub = c.subscriptionRepository.getById(id) ?: return
        if (!sub.active) return
        val cur = c.preferencesRepository.preferences.first().currencyCode
        val due = if (dueDay == NO_DUE_DAY) sub.nextDueDate else LocalDate.ofEpochDay(dueDay)
        val title = when (daysBefore) {
            0 -> "${sub.name} is due today"
            1 -> "${sub.name} is due tomorrow"
            else -> "${sub.name} is due in $daysBefore days"
        }
        val details = "${CurrencyFormatter.withSymbol(sub.amount, cur)} · ${sub.billingCycle.name.lowercase()} subscription" +
            if (daysBefore > 0) " · due ${DateUtils.shortDate(due)}" else ""
        NotificationHelper.show(
            app, NOTIF_SUBSCRIPTION_BASE + id.toInt(), title, details,
            openIntent = NotificationHelper.linkIntent(app, AppLink.Subscriptions(id)),
        )
        val latest = if (daysBefore == 0) {
            sub.copy(nextDueDate = AlarmTimes.advance(due, sub.billingCycle)).also { c.subscriptionRepository.update(it) }
        } else {
            sub
        }
        c.alarmScheduler.scheduleSubscription(latest)
    }

    private suspend fun fireReminder(app: SyncSpendApp, id: Long) {
        val c = app.container
        val reminder = c.reminderRepository.getById(id) ?: return
        if (!reminder.active) return
        NotificationHelper.show(
            app, NOTIF_REMINDER_BASE + id.toInt(), reminder.label, "Reminder from SyncSpend",
            openIntent = NotificationHelper.linkIntent(app, AppLink.Reminders(id)),
        )
        val next = if (reminder.schedule == ReminderSchedule.ONCE) {
            reminder.copy(active = false)
        } else {
            reminder.copy(nextTriggerDate = AlarmTimes.advance(reminder.nextTriggerDate, reminder.schedule))
        }
        c.reminderRepository.update(next)
        c.alarmScheduler.scheduleReminder(next)
    }

    companion object {
        /** The daily log reminder's buttons: "Add" opens Add Entry (and clears it), "Done" just clears it. */
        internal fun dailyActions(context: Context): List<NotificationHelper.Action> = listOf(
            NotificationHelper.linkAction(context, NOTIF_DAILY, "Add", AppLink.AddEntry),
            NotificationActionReceiver.dismissAction(context, NOTIF_DAILY, "Done"),
        )

        internal const val NOTIF_DAILY = 1000
        const val EXTRA_TYPE = "alarm_type"
        const val EXTRA_ID = "alarm_id"
        /** Subscriptions only: how many days ahead of the due day this alert is (0 = the day itself). */
        const val EXTRA_DAYS_BEFORE = "alarm_days_before"
        /** Subscriptions only: the due day (epoch day) this alert belongs to. */
        const val EXTRA_DUE_DAY = "alarm_due_day"
        private const val NO_DUE_DAY = Long.MIN_VALUE
        const val TYPE_DAILY = "daily"
        const val TYPE_SUBSCRIPTION = "subscription"
        const val TYPE_REMINDER = "reminder"
        private const val NOTIF_SUBSCRIPTION_BASE = 200_000
        private const val NOTIF_REMINDER_BASE = 300_000
    }
}
