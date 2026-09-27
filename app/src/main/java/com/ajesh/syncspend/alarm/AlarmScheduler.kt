package com.ajesh.syncspend.alarm

import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity

/** Real AlarmManager-backed scheduling for the daily reminder, subscriptions and custom reminders. */
interface AlarmScheduler {
    fun scheduleDailyReminder(minuteOfDay: Int)
    fun cancelDailyReminder()
    fun scheduleSubscription(subscription: SubscriptionEntity)
    fun cancelSubscription(id: Long)
    fun scheduleReminder(reminder: ReminderEntity)
    fun cancelReminder(id: Long)

    /** Always on, no user toggle: keeps the home-screen widget's "today" total from going stale overnight. */
    fun scheduleMidnightWidgetRefresh()

    /** Re-arms everything from the database + preferences (boot, app update, cold start). */
    suspend fun rescheduleAll()
}
