package com.ajesh.syncspend.domain.model

import com.ajesh.syncspend.data.db.entity.CategoryEntity

data class DefaultCategory(val name: String, val type: FlowType, val iconKey: String)

/**
 * The starter categories every install gets (the same set the previous tracker
 * shipped, so imported data lines up with them). They are ordinary categories
 * afterwards: rename, reorder or delete them freely on the Categories page.
 */
object DefaultCategories {
    val all: List<DefaultCategory> = listOf(
        DefaultCategory("Food", FlowType.EXPENSE, "utensils"),
        DefaultCategory("Groceries", FlowType.EXPENSE, "cart"),
        DefaultCategory("Transportation / Fuel", FlowType.EXPENSE, "fuel"),
        DefaultCategory("Recharge, Bills & Subscriptions", FlowType.EXPENSE, "receipt"),
        DefaultCategory("Entertainment", FlowType.EXPENSE, "film"),
        DefaultCategory("Home", FlowType.EXPENSE, "house"),
        DefaultCategory("Savings", FlowType.EXPENSE, "coin"),
        DefaultCategory("Debt / Liability", FlowType.EXPENSE, "card"),
        DefaultCategory("Salary", FlowType.INCOME, "bank"),
        DefaultCategory("Freelance", FlowType.INCOME, "brief"),
        DefaultCategory("Gift", FlowType.INCOME, "gift"),
        // Deliberately not named "Savings" — the existing Expense category above already has that
        // name and is left untouched; these are distinct, new, top-level-Savings-type categories.
        // Icon keys match CategoryIconGuesser's own keyword guess for each name (verified by
        // DefaultCategoriesTest) rather than being picked independently.
        DefaultCategory("Savings Goals", FlowType.SAVINGS, "coin"),
        DefaultCategory("Emergency Fund", FlowType.SAVINGS, "shield"),
        DefaultCategory("Investments", FlowType.SAVINGS, "trend"),
    )

    /**
     * The defaults not yet present, matched by (type, name) case-insensitively. An archived
     * row counts as present — a default the user deleted earlier is not brought back.
     */
    fun missingFrom(existing: List<CategoryEntity>): List<DefaultCategory> {
        val have = existing.mapTo(HashSet()) { it.type to it.name.trim().lowercase() }
        return all.filter { (it.type to it.name.lowercase()) !in have }
    }
}
