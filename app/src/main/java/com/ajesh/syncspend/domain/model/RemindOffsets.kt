package com.ajesh.syncspend.domain.model

/**
 * The "remind me N days before" choices of a subscription, stored as comma-separated day counts
 * ("1,3"). The due day itself always notifies and is not part of the set.
 */
object RemindOffsets {
    const val DEFAULT_TEXT = "1,3"
    val CHOICES = listOf(1, 3, 7)
    val DEFAULT = listOf(1, 3)

    /** Ascending, de-duplicated, positive day counts; junk entries are ignored. */
    fun parse(text: String): List<Int> =
        text.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }.distinct().sorted()

    fun format(days: Collection<Int>): String = days.filter { it > 0 }.distinct().sorted().joinToString(",")
}
