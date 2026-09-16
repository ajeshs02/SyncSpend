package com.ajesh.syncspend.util

import com.ajesh.syncspend.domain.model.CurrencyCode
import java.util.Locale
import kotlin.math.abs

/** Matches the design's `Math.abs(n).toLocaleString('en-US', {2 decimals})` formatting. */
object CurrencyFormatter {
    fun amount(value: Double): String = String.format(Locale.US, "%,.2f", abs(value))

    fun withSymbol(value: Double, currency: CurrencyCode): String =
        currency.symbol + amount(value)
}
