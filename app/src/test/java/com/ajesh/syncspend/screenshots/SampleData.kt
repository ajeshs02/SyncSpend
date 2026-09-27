package com.ajesh.syncspend.screenshots

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.di.DefaultAppContainer
import com.ajesh.syncspend.domain.model.FlowType
import java.time.LocalDate
import kotlinx.coroutines.runBlocking

/** A real container on Robolectric's SQLite with a few categories and entries, so whole screens can be rendered. */
fun sampleContainer(): DefaultAppContainer {
    val container = DefaultAppContainer(ApplicationProvider.getApplicationContext<Application>())
    runBlocking {
        val food = container.categoryRepository.createNext("Food", "utensils", FlowType.EXPENSE)
        val groceries = container.categoryRepository.createNext("Groceries", "cart", FlowType.EXPENSE)
        val fuel = container.categoryRepository.createNext("Transportation / Fuel", "fuel", FlowType.EXPENSE)
        val bills = container.categoryRepository.createNext("Recharge, Bills & Subscriptions", "receipt", FlowType.EXPENSE)
        val salary = container.categoryRepository.createNext("Salary", "bank", FlowType.INCOME)
        val today = LocalDate.now()
        var order = 0L
        // [note] is the entry's optional note (blank = none); [time] is its minute-of-day when known.
        suspend fun add(amount: Double, note: String, cat: Long, daysAgo: Long, time: Int? = null) = container.transactionRepository.insert(
            TransactionEntity(amount = amount, description = note, categoryId = cat, date = today.minusDays(daysAgo), createdAt = order++, timeMinuteOfDay = time),
        )
        add(-250.0, "Coffee and snacks with Rohan", food.id, 0, 9 * 60 + 12)
        add(-1241.0, "", groceries.id, 0, 18 * 60 + 40)
        add(-2100.0, "", fuel.id, 1)
        add(-399.0, "Monthly plan", bills.id, 1, 7 * 60 + 5)
        add(-180.0, "Team lunch at the new place near office", food.id, 3)
        add(-3600.0, "", groceries.id, 5)
        add(-450.0, "Dinner out", food.id, 9)
        add(-1500.0, "", bills.id, 12)
        add(-980.0, "", fuel.id, 20)
        add(-320.0, "", food.id, 33)
        add(85000.0, "", salary.id, 26)
    }
    return container
}
