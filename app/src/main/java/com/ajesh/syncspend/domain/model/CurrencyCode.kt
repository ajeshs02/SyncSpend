package com.ajesh.syncspend.domain.model

/** The 4 currencies offered in Settings, matching the design's currency row. */
enum class CurrencyCode(val symbol: String) {
    INR("₹"),
    USD("$"),
    EUR("€"),
    GBP("£"),
}
