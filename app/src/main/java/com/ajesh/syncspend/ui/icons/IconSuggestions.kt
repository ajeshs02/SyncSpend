package com.ajesh.syncspend.ui.icons

import com.ajesh.syncspend.csv.CategoryIconGuesser

/** What an icon is being chosen for; it decides the suggestions shown before the user has typed anything useful. */
enum class IconKind { CATEGORY, SUBSCRIPTION, REMINDER }

/**
 * The short "Suggested" row above the full icon grid: icons that match the name typed so far, then a few
 * that suit the kind of thing being created (a bell and a clock for a reminder, ...).
 */
object IconSuggestions {
    const val LIMIT = 5

    /** Words people actually type for subscriptions and reminders, ahead of the category keywords. */
    private val hints: List<Pair<List<String>, String>> = listOf(
        listOf("netflix", "hotstar", "prime video", "disney", "youtube", "movie", "cinema", "ott") to "film",
        listOf("spotify", "gaana", "wynk", "saavn", "music", "podcast") to "music",
        listOf("gym", "fitness", "yoga", "cult") to "dumbbell",
        listOf("wifi", "broadband", "internet", "fibre", "airtel", "jio") to "wifi",
        listOf("phone", "mobile", "recharge", "postpaid", "call") to "phone",
        listOf("rent", "house", "flat", "maintenance", "society") to "house",
        listOf("insurance", "policy", "premium", "lic") to "shield",
        listOf("emi", "loan", "credit card", "card") to "card",
        listOf("electric", "power", "water", "gas", "bijli") to "bolt",
        listOf("medicine", "pharmacy", "doctor", "pill") to "pill",
        listOf("school", "tuition", "course", "class", "fees") to "school",
        listOf("book", "kindle", "audible", "library") to "book",
        listOf("game", "gaming", "xbox", "playstation") to "gamepad",
        listOf("sip", "invest", "mutual", "stock", "fund") to "trend",
        listOf("salary", "payday", "pay day") to "bank",
        listOf("tax", "itr", "gst", "bill") to "receipt",
        listOf("birthday", "anniversary", "gift") to "gift",
        listOf("petrol", "fuel", "fastag", "car", "bike") to "car",
        listOf("meeting", "appointment", "visit") to "cal",
    )

    private fun defaults(kind: IconKind): List<String> = when (kind) {
        IconKind.REMINDER -> listOf("bell", "clock", "cal", "wallet", "receipt")
        IconKind.SUBSCRIPTION -> listOf("repeat", "card", "film", "music", "wifi")
        IconKind.CATEGORY -> emptyList()
    }

    /** Up to [limit] distinct icon keys, name matches first. Every key exists in [SyncSpendIcons.pickerKeys]. */
    fun suggest(kind: IconKind, name: String, limit: Int = LIMIT): List<String> {
        val n = name.trim().lowercase()
        val fromName = if (n.isEmpty()) emptyList() else hints.filter { (words, _) -> words.any { n.contains(it) } }.map { it.second }
        return (fromName + CategoryIconGuesser.guessAll(name) + defaults(kind))
            .filter { it in SyncSpendIcons.pickerKeys }
            .distinct()
            .take(limit)
    }
}
