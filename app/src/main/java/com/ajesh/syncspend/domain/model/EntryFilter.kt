package com.ajesh.syncspend.domain.model

/**
 * Matches the design's Entries-tab filter chips. Income only ever offers
 * [ALL] (the design forces this regardless of the stored filter). "This
 * Week" is a rolling 7-day window ending today, not an ISO calendar week
 * (judgment call — avoids Monday-start ambiguity). [CUSTOM] is a from/to
 * date range picked by the user.
 */
enum class EntryFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    CUSTOM("Custom"),
    ALL("All"),
}
