package com.ajesh.syncspend.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ajesh.syncspend.domain.model.CurrencyCode
import com.ajesh.syncspend.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

val Context.dataStore by preferencesDataStore(name = "syncspend_prefs")

/**
 * User preferences over a DataStore. The store is injected (the app passes [dataStore] of its
 * Context) so tests can hand in an isolated one.
 */
class PreferencesRepository(private val store: DataStore<Preferences>, scope: CoroutineScope) {
    constructor(context: Context, scope: CoroutineScope) : this(context.dataStore, scope)


    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_MINUTE_OF_DAY = intPreferencesKey("daily_reminder_minute_of_day")
        val NOTIF_PERMISSION_REQUESTED = booleanPreferencesKey("notif_permission_requested")
        val DEFAULT_CATEGORIES_SEEDED = booleanPreferencesKey("default_categories_seeded")
    }

    /**
     * Replays the latest value, so the first frame can already use the saved
     * theme (see [current]) instead of flashing the default and switching.
     */
    val preferences: SharedFlow<UserPreferences> = store.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            currencyCode = prefs[Keys.CURRENCY_CODE]?.let { runCatching { CurrencyCode.valueOf(it) }.getOrNull() }
                ?: CurrencyCode.INR,
            dailyReminderEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: false,
            dailyReminderMinuteOfDay = prefs[Keys.DAILY_REMINDER_MINUTE_OF_DAY] ?: (20 * 60),
            notifPermissionRequested = prefs[Keys.NOTIF_PERMISSION_REQUESTED] ?: false,
            defaultCategoriesSeeded = prefs[Keys.DEFAULT_CATEGORIES_SEEDED] ?: false,
        )
    }.shareIn(scope, SharingStarted.Eagerly, replay = 1)

    /** The latest loaded preferences, or null while DataStore is still reading. */
    fun current(): UserPreferences? = preferences.replayCache.firstOrNull()

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setCurrency(code: CurrencyCode) {
        store.edit { it[Keys.CURRENCY_CODE] = code.name }
    }

    suspend fun setDailyReminder(enabled: Boolean, minuteOfDay: Int) {
        store.edit {
            it[Keys.DAILY_REMINDER_ENABLED] = enabled
            it[Keys.DAILY_REMINDER_MINUTE_OF_DAY] = minuteOfDay
        }
    }

    suspend fun markDefaultCategoriesSeeded() {
        store.edit { it[Keys.DEFAULT_CATEGORIES_SEEDED] = true }
    }

    suspend fun setNotifPermissionRequested() {
        store.edit { it[Keys.NOTIF_PERMISSION_REQUESTED] = true }
    }
}
