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
    /**
     * Whether the Add Entry / widget keypad's "." key is enabled, so new entries can be given paise/
     * cents. Seeded from [currencyCode] the first time a currency is picked (false for INR, true for
     * every other currency) — see [PreferencesRepository.setCurrency] — but once the user has
     * explicitly set it from Settings, later currency changes never overwrite that explicit choice.
     * Never affects how an *existing* amount displays — [com.ajesh.syncspend.util.CurrencyFormatter]
     * always shows a value's real precision regardless of this flag.
     */
    val allowDecimalInput: Boolean = false,
    /** True once [allowDecimalInput] has been explicitly set by the user (see its own doc above). */
    val allowDecimalInputUserSet: Boolean = false,
)
