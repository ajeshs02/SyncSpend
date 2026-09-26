package com.ajesh.syncspend.navigation

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val ADD_ENTRY = "add_entry"
    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"

    const val SUBS_REMINDERS = "subs_reminders/{listMode}"
    const val SUBS_REMINDERS_PREFIX = "subs_reminders/"
    fun subsReminders(listMode: String) = "subs_reminders/$listMode"
}
