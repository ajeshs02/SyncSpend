package com.ajesh.syncspend.domain.model

/**
 * Where a tapped notification (or one of its buttons) should take the user. Carried in the launch intent
 * as a small string kind plus an optional row id, and turned back into a link by [parse].
 */
sealed interface AppLink {
    /** The Add Entry page. */
    data object AddEntry : AppLink

    /** The Subscriptions page, with [highlightId] (if any) moved to the top and outlined. */
    data class Subscriptions(val highlightId: Long?) : AppLink

    /** The Reminders page, with [highlightId] (if any) moved to the top and outlined. */
    data class Reminders(val highlightId: Long?) : AppLink

    companion object {
        const val EXTRA_KIND = "syncspend.link.kind"
        const val EXTRA_ID = "syncspend.link.id"
        /** A notification to dismiss once the link has been followed (its "Add" button). */
        const val EXTRA_CANCEL_NOTIFICATION = "syncspend.link.cancel_notification"

        const val KIND_ADD_ENTRY = "add_entry"
        const val KIND_SUBSCRIPTIONS = "subscriptions"
        const val KIND_REMINDERS = "reminders"

        fun kindOf(link: AppLink): String = when (link) {
            AddEntry -> KIND_ADD_ENTRY
            is Subscriptions -> KIND_SUBSCRIPTIONS
            is Reminders -> KIND_REMINDERS
        }

        fun idOf(link: AppLink): Long = when (link) {
            AddEntry -> NO_ID
            is Subscriptions -> link.highlightId ?: NO_ID
            is Reminders -> link.highlightId ?: NO_ID
        }

        const val NO_ID = -1L

        /** null for a missing or unknown kind (an ordinary launch). */
        fun parse(kind: String?, id: Long): AppLink? {
            val highlight = id.takeIf { it > 0 }
            return when (kind) {
                KIND_ADD_ENTRY -> AddEntry
                KIND_SUBSCRIPTIONS -> Subscriptions(highlight)
                KIND_REMINDERS -> Reminders(highlight)
                else -> null
            }
        }
    }
}
