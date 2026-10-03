package com.ajesh.syncspend.csv

/**
 * Picks a preset icon for a category created by an import (CSV files carry no
 * icons). First an exact map for the categories the previous tracker shipped,
 * then keyword matching, else the generic tag.
 */
object CategoryIconGuesser {
    private val exact = mapOf(
        "food" to "utensils",
        "groceries" to "cart",
        "transportation / fuel" to "fuel",
        "recharge, bills & subscriptions" to "receipt",
        "entertainment" to "film",
        "home" to "house",
        "savings" to "coin",
        "debt / liability" to "card",
        "salary" to "bank",
        "freelance" to "brief",
        "gift" to "gift",
        "pf" to "shield",
        "reward" to "spark",
        "previous savings" to "wallet",
        "other" to "tag",
        CsvImportParser.UNCATEGORIZED.lowercase() to "tag",
    )

    /** Ordered: the first entry with a matching keyword wins. */
    private val keywords: List<Pair<List<String>, String>> = listOf(
        listOf("coffee", "cafe", "tea") to "coffee",
        listOf("food", "restaurant", "dining", "lunch", "dinner", "breakfast", "snack", "swiggy", "zomato") to "utensils",
        listOf("grocer", "supermarket", "vegetable") to "cart",
        listOf("cloth", "apparel", "fashion") to "shirt",
        listOf("shop", "mall", "amazon", "flipkart") to "bag",
        listOf("fuel", "petrol", "diesel") to "fuel",
        listOf("transport", "taxi", "cab", "uber", "commute", "metro", "train", "bus") to "bus",
        listOf("car", "vehicle", "parking") to "car",
        listOf("travel", "trip", "flight", "holiday", "vacation") to "plane",
        listOf("internet", "broadband", "wifi") to "wifi",
        listOf("mobile", "recharge", "phone") to "phone",
        listOf("electric", "power") to "bolt",
        listOf("subscription", "netflix", "spotify", "prime") to "repeat",
        listOf("bill", "utilit", "tax") to "receipt",
        listOf("music") to "music",
        listOf("game", "gaming") to "gamepad",
        listOf("entertain", "movie", "cinema", "film") to "film",
        listOf("rent", "house", "home", "maintenance") to "house",
        listOf("health", "medic", "pharmac", "doctor", "hospital") to "pill",
        listOf("gym", "fitness", "sport") to "dumbbell",
        listOf("book") to "book",
        listOf("educat", "school", "course", "tuition", "college") to "school",
        listOf("insurance", "emergency") to "shield",
        listOf("invest", "stock", "mutual", "sip", "interest", "dividend") to "trend",
        listOf("saving", "deposit") to "coin",
        listOf("debt", "loan", "emi", "credit", "liabilit") to "card",
        listOf("salary", "wage", "payroll", "pay") to "bank",
        listOf("freelanc", "consult", "business", "work") to "brief",
        listOf("gift", "donation", "charity") to "gift",
        listOf("pet") to "heart",
    )

    fun guess(name: String): String = guessAll(name).firstOrNull() ?: "tag"

    /** Every icon whose keyword appears in [name], best match first (empty when nothing matches). */
    fun guessAll(name: String): List<String> {
        val n = name.trim().lowercase()
        if (n.isEmpty()) return emptyList()
        val matches = keywords.filter { (words, _) -> words.any { n.contains(it) } }.map { it.second }
        return (listOfNotNull(exact[n]) + matches).distinct()
    }
}
