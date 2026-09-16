package com.ajesh.syncspend.domain.model

/**
 * Matches the design's Entries-tab filter chips. Income only ever offers
 * [ALL] (the design forces this regardless of the stored filter). "This
 * Week"/"Last Week" are rolling 7-day windows from today, not ISO calendar
 * weeks (judgment call — avoids Monday-start ambiguity).
 */
enum class EntryFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    LAST_WEEK("Last Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    CUSTOM("Custom"),
    ALL("All"),
}
