package com.ajesh.syncspend.util

import com.ajesh.syncspend.domain.model.CurrencyCode
import kotlin.math.abs

/**
 * Money formatting: thousands-separated digits, and decimals only when the value actually has them
 * ("4,841" for a whole number, "4,840.50" for one that doesn't round to a whole unit) — a value's own
 * precision decides whether cents show, independent of whether paise/cents input is currently enabled
 * in Settings (that setting only gates the Add Entry keypad's "." key, never this display rule), so an
 * entry saved with decimals keeps showing them even after the setting is turned off.
 */
object CurrencyFormatter {
    /**
     * Rounds to the nearest cent once (`Math.round(abs(value) * 100)`), then splits into whole/cents —
     * avoids the float-boundary case where a separate `% 100` on the unrounded value could show "99"
     * cents for a value that's really 100 (i.e. the next whole unit).
     */
    fun amount(value: Double): String {
        val totalCents = Math.round(abs(value) * 100)
        val whole = totalCents / 100
        val cents = totalCents % 100
        val groupedWhole = groupDigits(whole.toString())
        return if (cents == 0L) groupedWhole else "$groupedWhole.${cents.toString().padStart(2, '0')}"
    }

    /**
     * Thousands-grouping for a plain digit string ("1234567" -> "1,234,567"), with no rounding or
     * parsing involved — shared by [amount] (which rounds a [Double] to whole digits first) and the
     * Add Entry / widget keypads, which already hold the amount as a typed digit string and just need
     * it grouped for display, never rounded.
     */
    fun groupDigits(digits: String): String {
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

    /**
     * Groups the integer part of a still-being-typed amount string ("1234.5" -> "1,234.5") for the
     * Add Entry / widget keypads — unlike [amount], this never rounds or pads: the user may still be
     * mid-entry (a trailing "." with no digits after it yet, or a single fractional digit).
     */
    fun groupTyped(raw: String): String {
        val dot = raw.indexOf('.')
        return if (dot < 0) groupDigits(raw) else groupDigits(raw.substring(0, dot)) + raw.substring(dot)
    }
}
