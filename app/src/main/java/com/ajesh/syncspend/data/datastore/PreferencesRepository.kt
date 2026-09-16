package com.ajesh.syncspend.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ajesh.syncspend.domain.model.CurrencyCode
import com.ajesh.syncspend.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "syncspend_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_MINUTE_OF_DAY = intPreferencesKey("daily_reminder_minute_of_day")
        val NOTIF_PERMISSION_REQUESTED = booleanPreferencesKey("notif_permission_requested")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            currencyCode = prefs[Keys.CURRENCY_CODE]?.let { runCatching { CurrencyCode.valueOf(it) }.getOrNull() }
                ?: CurrencyCode.INR,
            dailyReminderEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: false,
            dailyReminderMinuteOfDay = prefs[Keys.DAILY_REMINDER_MINUTE_OF_DAY] ?: (20 * 60),
            notifPermissionRequested = prefs[Keys.NOTIF_PERMISSION_REQUESTED] ?: false,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setCurrency(code: CurrencyCode) {
        context.dataStore.edit { it[Keys.CURRENCY_CODE] = code.name }
    }

    suspend fun setDailyReminder(enabled: Boolean, minuteOfDay: Int) {
        context.dataStore.edit {
            it[Keys.DAILY_REMINDER_ENABLED] = enabled
            it[Keys.DAILY_REMINDER_MINUTE_OF_DAY] = minuteOfDay
        }
    }

    suspend fun setNotifPermissionRequested() {
        context.dataStore.edit { it[Keys.NOTIF_PERMISSION_REQUESTED] = true }
    }
}
