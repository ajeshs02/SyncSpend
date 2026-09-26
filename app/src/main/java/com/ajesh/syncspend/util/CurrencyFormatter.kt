package com.ajesh.syncspend.util

import com.ajesh.syncspend.domain.model.CurrencyCode
import java.util.Locale
import kotlin.math.abs

/**
 * Whole-unit money formatting: no paise/cents anywhere in the app, so a figure is
 * always digits with thousands separators ("4,841"), never "4,840.50" or "85,000.00".
 */
object CurrencyFormatter {
    fun amount(value: Double): String = String.format(Locale.US, "%,.0f", abs(value))

    fun withSymbol(value: Double, currency: CurrencyCode): String =
        currency.symbol + amount(value)
}
