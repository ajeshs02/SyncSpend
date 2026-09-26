package com.ajesh.syncspend.data.datastore

import com.ajesh.syncspend.domain.model.CurrencyCode
import com.ajesh.syncspend.domain.model.ThemeMode

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currencyCode: CurrencyCode = CurrencyCode.INR,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderMinuteOfDay: Int = 20 * 60, // 8:00 PM default
    val notifPermissionRequested: Boolean = false,
    /** True once the starter categories have been added (they are added exactly once). */
    val defaultCategoriesSeeded: Boolean = false,
)
