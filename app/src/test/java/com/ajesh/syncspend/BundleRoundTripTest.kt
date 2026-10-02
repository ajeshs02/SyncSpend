package com.ajesh.syncspend

import android.app.Application
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajesh.syncspend.csv.BundleExporter
import com.ajesh.syncspend.csv.BundleImporter
import com.ajesh.syncspend.csv.BundleSection
import com.ajesh.syncspend.data.db.AppDatabase
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.ForecastEntity
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.ContributionKind
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.FundingSource
import com.ajesh.syncspend.domain.model.ReminderSchedule
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the full export -> import path through real files, proving the hardening the round's
 * plan asked for: every section round-trips, re-importing the same file is a no-op (duplicate
 * rule holds), a malformed bundle aborts and changes nothing, and a bundle missing the newer
 * Savings fields still imports cleanly (backward compatibility).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BundleRoundTripTest {
    @get:Rule val tmp = TemporaryFolder()
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var dbA: AppDatabase
    private lateinit var dbB: AppDatabase

    @Before fun setUp() {
        dbA = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        dbB = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun tearDown() {
        dbA.close()
        dbB.close()
    }

    private fun uriFor(name: String): Uri = Uri.fromFile(File(tmp.root, name))

    @Test fun everySectionRoundTripsIntoAFreshDatabase() = runBlocking {
        val day = LocalDate.of(2026, 9, 20)
        val foodCat = CategoryEntity(name = "Food", iconKey = "utensils", type = FlowType.EXPENSE, sortOrder = 0)
        val foodId = dbA.categoryDao().insert(foodCat)
        dbA.transactionDao().insert(
            TransactionEntity(amount = -120.5, description = "lunch", categoryId = foodId, date = day, createdAt = 1L, type = FlowType.EXPENSE, fundingSource = FundingSource.SAVINGS),
        )
        dbA.transactionDao().insert(
            TransactionEntity(amount = 6000.0, description = "", categoryId = foodId, date = day, createdAt = 2L, type = FlowType.SAVINGS, contributionKind = ContributionKind.TRANSFER),
        )
        dbA.reminderDao().insert(ReminderEntity(label = "Water the plants", iconKey = "bell", schedule = ReminderSchedule.DAILY, timeMinuteOfDay = 540, nextTriggerDate = day, active = true))
        dbA.subscriptionDao().insert(SubscriptionEntity(name = "Netflix", iconKey = "repeat", amount = 649.0, billingCycle = BillingCycle.MONTHLY, nextDueDate = day, categoryId = null, active = true))
        dbA.forecastDao().insert(ForecastEntity(note = "Rent", amount = 12000.0, date = null))

        val uri = uriFor("full.json")
        BundleExporter.export(
            context, uri, setOf(BundleSection.TRANSACTIONS, BundleSection.REMINDERS, BundleSection.SUBSCRIPTIONS, BundleSection.FORECASTS),
            dbA.transactionDao().getAllOnce(), dbA.categoryDao().getAllOnce(), dbA.reminderDao().getAllOnce(),
            dbA.subscriptionDao().getAllOnce(), dbA.forecastDao().getAllOnce(),
        )

        val result = BundleImporter.import(context, uri, dbB)
        assertNull(result.error)
        assertEquals(2, result.importedTransactions)
        assertEquals(1, result.importedReminders)
        assertEquals(1, result.importedSubscriptions)
        assertEquals(1, result.importedForecasts)

        val txs = dbB.transactionDao().getAllOnce()
        val expense = txs.single { it.type == FlowType.EXPENSE }
        assertEquals(-120.5, expense.amount, 0.0)
        assertEquals(FundingSource.SAVINGS, expense.fundingSource)
        val savings = txs.single { it.type == FlowType.SAVINGS }
        assertEquals(ContributionKind.TRANSFER, savings.contributionKind)
        assertNotNull(dbB.categoryDao().getAllOnce().find { it.name == "Food" })

        val reminder = dbB.reminderDao().getAllOnce().single()
        assertEquals("Water the plants", reminder.label)

        val sub = dbB.subscriptionDao().getAllOnce().single()
        assertEquals("Netflix", sub.name)

        val forecast = dbB.forecastDao().getAllOnce().single()
        assertEquals("Rent", forecast.note)
        assertNull(forecast.date)
    }

    @Test fun importingTheSameBundleTwiceAddsNothingTheSecondTime() = runBlocking {
        val day = LocalDate.of(2026, 9, 20)
        val cat = CategoryEntity(name = "Food", iconKey = "utensils", type = FlowType.EXPENSE, sortOrder = 0)
        val catId = dbA.categoryDao().insert(cat)
        dbA.transactionDao().insert(TransactionEntity(amount = -100.0, description = "", categoryId = catId, date = day, createdAt = 1L, type = FlowType.EXPENSE))
        dbA.forecastDao().insert(ForecastEntity(note = "Rent", amount = 12000.0, date = null))

        val uri = uriFor("dup.json")
        BundleExporter.export(
            context, uri, setOf(BundleSection.TRANSACTIONS, BundleSection.FORECASTS),
            dbA.transactionDao().getAllOnce(), dbA.categoryDao().getAllOnce(), emptyList(), emptyList(), dbA.forecastDao().getAllOnce(),
        )

        val first = BundleImporter.import(context, uri, dbB)
        assertEquals(1, first.importedTransactions)
        assertEquals(1, first.importedForecasts)

        val second = BundleImporter.import(context, uri, dbB)
        assertEquals(0, second.importedTransactions) // already present -> skipped as a duplicate
        assertEquals(0, second.importedForecasts)
        assertEquals(1, dbB.transactionDao().getAllOnce().size)
        assertEquals(1, dbB.forecastDao().getAllOnce().size)
    }

    @Test fun aMalformedBundleAbortsAndChangesNothing() = runBlocking {
        val uri = uriFor("broken.json")
        File(tmp.root, "broken.json").writeText(
            """{"schemaVersion":1,"transactions":[{"date":"not-a-date","type":"EXPENSE","category":"Food","description":"","amount":100.0}]}""",
        )
        val before = dbB.transactionDao().getAllOnce().size

        val result = BundleImporter.import(context, uri, dbB)
        assertNotNull(result.error)
        assertEquals(before, dbB.transactionDao().getAllOnce().size) // nothing written
    }

    @Test fun aPreSavingsBundleWithNoTypeFieldImportsUsingTheSignRule() = runBlocking {
        val uri = uriFor("legacy.json")
        File(tmp.root, "legacy.json").writeText(
            """{"schemaVersion":1,"transactions":[
                {"date":"2026-09-20","category":"Food","description":"","amount":-120.0},
                {"date":"2026-09-20","category":"Salary","description":"","amount":50000.0}
            ]}""".trimIndent(),
        )

        val result = BundleImporter.import(context, uri, dbB)
        assertNull(result.error)
        assertEquals(2, result.importedTransactions)
        val rows = dbB.transactionDao().getAllOnce()
        assertEquals(FlowType.EXPENSE, rows.single { it.amount < 0 }.type)
        assertEquals(FlowType.INCOME, rows.single { it.amount > 0 }.type)
    }
}
