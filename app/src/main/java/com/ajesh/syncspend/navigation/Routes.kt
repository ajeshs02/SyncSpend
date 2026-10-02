package com.ajesh.syncspend.navigation

import com.ajesh.syncspend.domain.model.FlowType

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val ADD_ENTRY = "add_entry"
    private const val CATEGORIES_BASE = "categories"
    const val CATEGORIES = "$CATEGORIES_BASE?flow={flow}"
    /** [flow] pre-selects the toggle (e.g. opened from Add Entry's "+ add category" shortcut); omit for the plain Settings entry point, which defaults to Expense. */
    fun categories(flow: FlowType? = null): String = CATEGORIES_BASE + (flow?.let { "?flow=${it.name}" } ?: "")
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val FORECAST = "forecast"

    const val SUBS_REMINDERS = "subs_reminders/{listMode}?highlight={highlight}"
    const val SUBS_REMINDERS_PREFIX = "subs_reminders/"
    /** [highlightId] (a subscription or reminder id) is moved to the top of the list and outlined. */
    fun subsReminders(listMode: String, highlightId: Long? = null) =
        "subs_reminders/$listMode" + (highlightId?.let { "?highlight=$it" } ?: "")
}
