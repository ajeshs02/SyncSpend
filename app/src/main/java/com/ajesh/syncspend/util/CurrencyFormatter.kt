package com.ajesh.syncspend.util

import com.ajesh.syncspend.domain.model.CurrencyCode
import kotlin.math.abs

/**
 * Whole-unit money formatting: no paise/cents anywhere in the app, so a figure is
 * always digits with thousands separators ("4,841"), never "4,840.50" or "85,000.00".
 */
object CurrencyFormatter {
    /**
     * Same text as `String.format(Locale.US, "%,.0f", abs(value))` (halves round up), built by hand:
     * a `Formatter` per figure is slow and allocation-heavy, and a long list formats hundreds of them.
     */
    fun amount(value: Double): String {
        val digits = Math.round(abs(value)).toString()
        if (digits.length <= 3) return digits
        val out = StringBuilder(digits.length + (digits.length - 1) / 3)
        val lead = digits.length % 3
        if (lead > 0) out.append(digits, 0, lead)
        var i = lead
        while (i < digits.length) {
            if (out.isNotEmpty()) out.append(',')
            out.append(digits, i, i + 3)
            i += 3
        }
        return out.toString()
    }

    fun withSymbol(value: Double, currency: CurrencyCode): String =
        currency.symbol + amount(value)
}
