package com.ajesh.syncspend.domain.model

/**
 * Rules for an entry's optional note (stored in `TransactionEntity.description`). The length cap keeps a
 * note within the two lines a list row gives it (it wraps, it is never cut off), so it only limits *typing*:
 * a longer note that already exists (an import) is shown in full and can still be edited down.
 */
object EntryNote {
    const val MAX_LENGTH = 40

    /** True when [proposed] may replace [current] in an input field. */
    fun accepts(current: String, proposed: String): Boolean =
        proposed.length <= MAX_LENGTH || proposed.length < current.length

    /** What is stored: trimmed, and never null. Blank means "no note". */
    fun normalize(text: String): String = text.trim()
}
