package com.ajesh.syncspend.ui.home

import com.ajesh.syncspend.util.CurrencyFormatter
import kotlin.random.Random

/** The little line under the hero total that cycles through a few averages. Pure, so it is easy to test. */
internal object HomeInsights {
    /** How long each line stays before the next one fades in. */
    const val ROTATE_MILLIS = 5_000L

    /**
     * "Averaging ₹X per entry / per day / per week" for a period with [entryCount] entries totalling [total]
     * over [days] days. Empty when there is nothing to average.
     */
    fun build(total: Double, entryCount: Int, days: Int, currencySymbol: String): List<String> {
        if (entryCount <= 0 || days <= 0) return emptyList()
        // Rounded to a whole unit before formatting: these are derived averages (almost never a round
        // number on their own), not stored amounts, so CurrencyFormatter's "show real precision" rule
        // doesn't apply here — an approximate hint line reads better as a whole figure.
        fun avg(value: Double) = "Averaging $currencySymbol${CurrencyFormatter.amount(Math.round(value).toDouble())}"
        val perDay = total / days
        return listOf(
            "${avg(total / entryCount)} per entry",
            "${avg(perDay)} per day",
            "${avg(perDay * 7)} per week",
        )
    }

    /** A random line other than [current], so the text always visibly changes. */
    fun nextIndex(current: Int, size: Int, random: Random = Random): Int {
        if (size <= 1) return 0
        val pick = random.nextInt(size - 1)
        return if (pick >= current) pick + 1 else pick
    }
}
