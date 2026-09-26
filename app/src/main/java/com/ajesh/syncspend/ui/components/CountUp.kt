package com.ajesh.syncspend.ui.components

import kotlin.math.pow

/** One character of the counting figure; [visible] is false for leading zeros (drawn transparent so widths stay put). */
internal data class CountCell(val char: Char, val visible: Boolean)

/**
 * The maths of the Home total's count-up, kept free of Compose so it can be tested.
 *
 * The final figure (e.g. "12,345.67") is laid out once. Every digit place then runs
 * its *own* ease-out over its own duration: the leftmost digit finishes after
 * [LEFT_MS], each place to the right takes longer, up to [RIGHT_MS] for the last.
 * At any instant a place shows the digit that the *value so far* (final amount ×
 * that place's eased progress) has in that place — so digits lock in from left to
 * right while the rightmost keep rolling and decelerating, like an odometer coming
 * to rest.
 */
internal object CountUp {
    const val LEFT_MS = 1100f
    const val RIGHT_MS = 2400f

    /** Time until every digit has settled. */
    const val TOTAL_MS = RIGHT_MS

    /** How long digit [rank] (0 = leftmost) of [count] digits takes. */
    fun durationMs(rank: Int, count: Int): Float =
        if (count <= 1) RIGHT_MS else LEFT_MS + (RIGHT_MS - LEFT_MS) * rank / (count - 1)

    /** Cubic ease-out: 0 -> 0 and 1 -> 1 exactly, brisk at first and then slowing smoothly to a stop. */
    fun easeOut(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        val inv = 1f - x
        return 1f - inv * inv * inv
    }

    /** The figure as it looks [elapsedMs] into the animation. [finalText] is a formatted amount like "1,234.56". */
    fun frame(finalText: String, elapsedMs: Float): List<CountCell> {
        val digitCount = finalText.count { it.isDigit() }
        val cents = finalText.filter { it.isDigit() }.toLongOrNull() ?: 0L
        val cells = ArrayList<CountCell>(finalText.length)
        var rank = 0
        var digitsRight = digitCount
        finalText.forEach { ch ->
            if (ch.isDigit()) {
                digitsRight -= 1 // digits still to the right of this one = its decimal place (0 = last cent digit)
                val progress = easeOut(elapsedMs / durationMs(rank, digitCount))
                val value = (cents * progress.toDouble()).toLong()
                val scale = 10.0.pow(digitsRight).toLong()
                // The units digit and the two decimals always show, so the figure never collapses to nothing.
                val visible = digitsRight <= 2 || value >= scale
                cells += CountCell(((value / scale) % 10).toInt().digitToChar(), visible)
                rank += 1
            } else {
                // A comma is only worth showing once the digit to its left is.
                cells += CountCell(ch, ch == '.' || cells.lastOrNull()?.visible == true)
            }
        }
        return cells
    }

    /** [frame] as plain text with hidden cells as spaces. */
    fun frameText(finalText: String, elapsedMs: Float): String =
        frame(finalText, elapsedMs).joinToString("") { if (it.visible) it.char.toString() else " " }
}
