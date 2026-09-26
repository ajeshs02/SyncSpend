package com.ajesh.syncspend.alarm

import com.ajesh.syncspend.data.datastore.PreferencesRepository
import kotlinx.coroutines.flow.first

/**
 * The single owner of the daily "log today's spend" reminder. Settings and the
 * Reminders list both drive it through here, and both read the same DataStore
 * values, so flipping it on one screen is reflected on the other.
 */
class DailyReminderManager(
    private val preferences: PreferencesRepository,
    private val scheduler: AlarmScheduler,
) {
    suspend fun setEnabled(enabled: Boolean) {
        val minute = preferences.preferences.first().dailyReminderMinuteOfDay
        preferences.setDailyReminder(enabled, minute)
        if (enabled) scheduler.scheduleDailyReminder(minute) else scheduler.cancelDailyReminder()
    }

    /** Changing the time also turns the reminder on, like picking a time in the design. */
    suspend fun setTime(minuteOfDay: Int) {
        preferences.setDailyReminder(true, minuteOfDay)
        scheduler.scheduleDailyReminder(minuteOfDay)
    }
}
