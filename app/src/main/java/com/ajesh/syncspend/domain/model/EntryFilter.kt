package com.ajesh.syncspend.domain.model

/**
 * The date chips on the Transactions Entries and Categories tabs, for both Expense and Income.
 * [THIS_WEEK] runs Monday to Sunday (weeks start on Monday). [CUSTOM] is a from/to date range picked by
 * the user. The date range each one covers is [com.ajesh.syncspend.domain.analytics.AnalyticsEngine.entryFilterRange].
 */
enum class EntryFilter(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    CUSTOM("Custom"),
}
