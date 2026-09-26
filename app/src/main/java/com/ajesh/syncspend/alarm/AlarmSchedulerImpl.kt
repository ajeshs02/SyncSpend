package com.ajesh.syncspend.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.data.repository.ReminderRepository
import com.ajesh.syncspend.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first

/**
 * One-shot exact alarms that re-arm themselves from [AlarmReceiver] (not
 * `setRepeating`, which is inexact by design). Exact scheduling is used when
 * the OS allows it; otherwise it degrades to `setAndAllowWhileIdle` instead of
 * failing. Every alarm has its own data URI so PendingIntents never collide.
 */
class AlarmSchedulerImpl(
    private val context: Context,
    private val subscriptions: SubscriptionRepository,
    private val reminders: ReminderRepository,
    private val prefs: PreferencesRepository,
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(type: String, id: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setData(Uri.parse("syncspend://alarm/$type/$id"))
            .putExtra(AlarmReceiver.EXTRA_TYPE, type)
            .putExtra(AlarmReceiver.EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun arm(triggerAtMillis: Long, pi: PendingIntent) {
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (exactAllowed) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun cancel(type: String, id: Long) = alarmManager.cancel(pendingIntent(type, id))

    override fun scheduleDailyReminder(minuteOfDay: Int) =
        arm(AlarmTimes.nextDaily(minuteOfDay), pendingIntent(AlarmReceiver.TYPE_DAILY, 0))

    override fun cancelDailyReminder() = cancel(AlarmReceiver.TYPE_DAILY, 0)

    override fun scheduleSubscription(subscription: SubscriptionEntity) {
        if (!subscription.active) return cancelSubscription(subscription.id)
        arm(
            AlarmTimes.nextSubscription(subscription.nextDueDate, subscription.billingCycle),
            pendingIntent(AlarmReceiver.TYPE_SUBSCRIPTION, subscription.id),
        )
    }

    override fun cancelSubscription(id: Long) = cancel(AlarmReceiver.TYPE_SUBSCRIPTION, id)

    override fun scheduleReminder(reminder: ReminderEntity) {
        if (!reminder.active) return cancelReminder(reminder.id)
        val at = AlarmTimes.nextReminder(reminder.nextTriggerDate, reminder.schedule, reminder.timeMinuteOfDay)
        if (at == null) cancelReminder(reminder.id) else arm(at, pendingIntent(AlarmReceiver.TYPE_REMINDER, reminder.id))
    }

    override fun cancelReminder(id: Long) = cancel(AlarmReceiver.TYPE_REMINDER, id)

    override suspend fun rescheduleAll() {
        val p = prefs.preferences.first()
        if (p.dailyReminderEnabled) scheduleDailyReminder(p.dailyReminderMinuteOfDay) else cancelDailyReminder()
        subscriptions.getActiveOnce().forEach(::scheduleSubscription)
        reminders.getActiveOnce().forEach(::scheduleReminder)
    }
}
