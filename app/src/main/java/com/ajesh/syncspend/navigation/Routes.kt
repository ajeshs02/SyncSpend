package com.ajesh.syncspend.navigation

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val ADD_ENTRY = "add_entry"
    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"

    // Added in Phase 8b, kept here as a forward reference for the route table.
    const val SUBS_REMINDERS = "subs_reminders/{listMode}"
    fun subsReminders(listMode: String) = "subs_reminders/$listMode"
}
