package com.ajesh.syncspend.navigation

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val ADD_ENTRY = "add_entry"
    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"

    const val SUBS_REMINDERS = "subs_reminders/{listMode}?highlight={highlight}"
    const val SUBS_REMINDERS_PREFIX = "subs_reminders/"
    /** [highlightId] (a subscription or reminder id) is moved to the top of the list and outlined. */
    fun subsReminders(listMode: String, highlightId: Long? = null) =
        "subs_reminders/$listMode" + (highlightId?.let { "?highlight=$it" } ?: "")
}
