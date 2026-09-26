package com.ajesh.syncspend.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ajesh.syncspend.SyncSpendApp
import com.ajesh.syncspend.domain.model.ReminderSchedule
import com.ajesh.syncspend.util.CurrencyFormatter
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
        val app = context.applicationContext as SyncSpendApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (type) {
                    TYPE_DAILY -> fireDaily(app)
                    TYPE_SUBSCRIPTION -> fireSubscription(app, id)
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
        NotificationHelper.show(
            app, NOTIF_DAILY, "Log today's spend",
            "Take a moment to record what you spent or earned today.",
        )
        c.alarmScheduler.scheduleDailyReminder(prefs.dailyReminderMinuteOfDay)
    }

    private suspend fun fireSubscription(app: SyncSpendApp, id: Long) {
        val c = app.container
        val sub = c.subscriptionRepository.getById(id) ?: return
        if (!sub.active) return
        val cur = c.preferencesRepository.preferences.first().currencyCode
        NotificationHelper.show(
            app, NOTIF_SUBSCRIPTION_BASE + id.toInt(), "${sub.name} is due",
            "${CurrencyFormatter.withSymbol(sub.amount, cur)} · ${sub.billingCycle.name.lowercase()} subscription",
        )
        val advanced = sub.copy(nextDueDate = AlarmTimes.advance(sub.nextDueDate, sub.billingCycle))
        c.subscriptionRepository.update(advanced)
        c.alarmScheduler.scheduleSubscription(advanced)
    }

    private suspend fun fireReminder(app: SyncSpendApp, id: Long) {
        val c = app.container
        val reminder = c.reminderRepository.getById(id) ?: return
        if (!reminder.active) return
        NotificationHelper.show(app, NOTIF_REMINDER_BASE + id.toInt(), reminder.label, "Reminder from SyncSpend")
        val next = if (reminder.schedule == ReminderSchedule.ONCE) {
            reminder.copy(active = false)
        } else {
            reminder.copy(nextTriggerDate = AlarmTimes.advance(reminder.nextTriggerDate, reminder.schedule))
        }
        c.reminderRepository.update(next)
        c.alarmScheduler.scheduleReminder(next)
    }

    companion object {
        const val EXTRA_TYPE = "alarm_type"
        const val EXTRA_ID = "alarm_id"
        const val TYPE_DAILY = "daily"
        const val TYPE_SUBSCRIPTION = "subscription"
        const val TYPE_REMINDER = "reminder"
        private const val NOTIF_DAILY = 1000
        private const val NOTIF_SUBSCRIPTION_BASE = 200_000
        private const val NOTIF_REMINDER_BASE = 300_000
    }
}
