package com.ajesh.syncspend

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.ajesh.syncspend.data.db.AppDatabase
import com.ajesh.syncspend.data.db.MIGRATION_3_4
import com.ajesh.syncspend.data.db.MIGRATION_4_5
import com.ajesh.syncspend.data.db.MIGRATION_5_6
import com.ajesh.syncspend.data.db.MIGRATION_6_7
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.FlowType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every migration from the hand-built v3 fixture up to the current schema — the real path any installed app takes. */
private val ALL_MIGRATIONS = arrayOf(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)

/**
 * The real user database is at v3 and holds real entries, so the v3 -> v4 migration is tested against a
 * hand-built v3 file (the exact DDL Room generated for v3). Opening it through Room also runs Room's own
 * schema validation, which fails if the migrated tables differ from the v4 entities.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val zone = ZoneId.systemDefault()

    private fun millis(date: LocalDate, time: LocalTime) = date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    private fun createV3(name: String, fill: (SupportSQLiteDatabase) -> Unit) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(
                object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `iconKey` TEXT NOT NULL, `type` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `archived` INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `subscriptions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `iconKey` TEXT NOT NULL, `amount` REAL NOT NULL, `billingCycle` TEXT NOT NULL, `nextDueDate` INTEGER NOT NULL, `categoryId` INTEGER, `active` INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `label` TEXT NOT NULL, `iconKey` TEXT NOT NULL, `schedule` TEXT NOT NULL, `timeMinuteOfDay` INTEGER NOT NULL, `nextTriggerDate` INTEGER NOT NULL, `active` INTEGER NOT NULL)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            ).build(),
        )
        helper.writableDatabase.also(fill)
        helper.close()
    }

    private fun createV5(name: String, fill: (SupportSQLiteDatabase) -> Unit) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(
                object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `iconKey` TEXT NOT NULL, `type` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `archived` INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, `description` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `timeMinuteOfDay` INTEGER)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `subscriptions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `iconKey` TEXT NOT NULL, `amount` REAL NOT NULL, `billingCycle` TEXT NOT NULL, `nextDueDate` INTEGER NOT NULL, `categoryId` INTEGER, `active` INTEGER NOT NULL, `remindDaysBefore` TEXT NOT NULL DEFAULT '1,3', `remindMinuteOfDay` INTEGER NOT NULL DEFAULT 540)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `label` TEXT NOT NULL, `iconKey` TEXT NOT NULL, `schedule` TEXT NOT NULL, `timeMinuteOfDay` INTEGER NOT NULL, `nextTriggerDate` INTEGER NOT NULL, `active` INTEGER NOT NULL)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            ).build(),
        )
        helper.writableDatabase.also(fill)
        helper.close()
    }

    private fun tx(db: SupportSQLiteDatabase, id: Long, description: String, categoryId: Long, date: LocalDate, createdAt: Long) {
        db.execSQL(
            "INSERT INTO transactions (id, amount, description, categoryId, date, createdAt) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any>(id, -100.0, description, categoryId, date.toEpochDay(), createdAt),
        )
    }

    @Test fun migratesNotesTimesAndSubscriptionReminders() = runBlocking {
        val name = "migration-test.db"
        val day = LocalDate.of(2026, 9, 20)
        createV3(name) { db ->
            db.execSQL("INSERT INTO categories VALUES (1, 'Food', 'utensils', 'EXPENSE', 0, 0)")
            db.execSQL("INSERT INTO categories VALUES (2, 'Groceries', 'cart', 'EXPENSE', 1, 0)")
            // 1: auto-filled with its category name, logged on its own day at 14:35 -> blank note, time 14:35
            tx(db, 1, "Food", 1, day, millis(day, LocalTime.of(14, 35)))
            // 2: a real note, logged three days later (back-dated) -> note kept, no time
            tx(db, 2, "Lunch with team", 1, day, millis(day.plusDays(3), LocalTime.of(9, 0)))
            // 3: an entry whose category no longer exists -> description kept
            tx(db, 3, "Ghost", 99, day, millis(day, LocalTime.of(0, 0)))
            // 4: differs from the category name only by case -> kept (only exact copies are blanked)
            tx(db, 4, "groceries", 2, day, millis(day.minusDays(1), LocalTime.of(23, 59)))
            // 5: logged just before midnight on its own day -> time 23:59
            tx(db, 5, "Groceries", 2, day, millis(day, LocalTime.of(23, 59)))
            db.execSQL("INSERT INTO subscriptions VALUES (1, 'Netflix', 'repeat', 649.0, 'MONTHLY', ${day.toEpochDay()}, NULL, 1)")
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS).allowMainThreadQueries().build()
        try {
            val rows = db.transactionDao().getAllOnce().associateBy { it.id }
            assertEquals("", rows.getValue(1).description)
            assertEquals(14 * 60 + 35, rows.getValue(1).timeMinuteOfDay)
            assertEquals("Lunch with team", rows.getValue(2).description)
            assertNull(rows.getValue(2).timeMinuteOfDay)
            assertEquals("Ghost", rows.getValue(3).description)
            assertEquals(0, rows.getValue(3).timeMinuteOfDay) // midnight on its own day is a real time
            assertEquals("groceries", rows.getValue(4).description)
            assertNull(rows.getValue(4).timeMinuteOfDay)
            assertEquals("", rows.getValue(5).description)
            assertEquals(23 * 60 + 59, rows.getValue(5).timeMinuteOfDay)
            assertEquals(5, rows.size)
            assertEquals(-100.0, rows.getValue(1).amount, 0.0)

            val sub = db.subscriptionDao().getActiveOnce().single()
            assertEquals("Netflix", sub.name)
            assertEquals("1,3", sub.remindDaysBefore)
            assertEquals(649.0, sub.amount, 0.0)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun anEmptyV3DatabaseMigrates() = runBlocking {
        val name = "migration-empty.db"
        createV3(name) { }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS).allowMainThreadQueries().build()
        try {
            assertEquals(0, db.transactionDao().getAllOnce().size)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun migration5to6BackfillsTypeAndAddsSavingsCategoriesWithoutTouchingTheExistingOne() = runBlocking {
        val name = "migration-savings.db"
        val day = LocalDate.of(2026, 9, 20)
        createV5(name) { db ->
            db.execSQL("INSERT INTO categories VALUES (1, 'Food', 'utensils', 'EXPENSE', 0, 0)")
            db.execSQL("INSERT INTO categories VALUES (2, 'Salary', 'bank', 'INCOME', 0, 0)")
            // The pre-existing Expense category literally named "Savings" — must survive untouched.
            db.execSQL("INSERT INTO categories VALUES (3, 'Savings', 'coin', 'EXPENSE', 6, 0)")
            db.execSQL("INSERT INTO transactions (id, amount, description, categoryId, date, createdAt, timeMinuteOfDay) VALUES (1, -250.0, '', 1, ${day.toEpochDay()}, 1, NULL)")
            db.execSQL("INSERT INTO transactions (id, amount, description, categoryId, date, createdAt, timeMinuteOfDay) VALUES (2, 50000.0, '', 2, ${day.toEpochDay()}, 2, NULL)")
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7).allowMainThreadQueries().build()
        try {
            val rows = db.transactionDao().getAllOnce().associateBy { it.id }
            assertEquals(FlowType.EXPENSE, rows.getValue(1).type)
            assertEquals(FlowType.INCOME, rows.getValue(2).type)
            assertNull(rows.getValue(1).fundingSource)
            assertNull(rows.getValue(2).contributionKind)

            val categories = db.categoryDao().getAllOnce()
            val oldSavings = categories.single { it.name == "Savings" }
            assertEquals(FlowType.EXPENSE, oldSavings.type) // untouched, not renamed or migrated
            assertEquals("coin", oldSavings.iconKey)

            val newSavingsCategories = categories.filter { it.type == FlowType.SAVINGS }
            assertEquals(setOf("Savings Goals", "Emergency Fund", "Investments"), newSavingsCategories.map { it.name }.toSet())
            assertTrue(newSavingsCategories.all { !it.archived })

            // MIGRATION_6_7 created the forecasts table in the same upgrade chain — usable immediately.
            val forecastId = db.forecastDao().insert(com.ajesh.syncspend.data.db.entity.ForecastEntity(note = "Rent", amount = 12000.0, date = null))
            assertEquals(1, db.forecastDao().getAllOnce().filter { it.id == forecastId }.size)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun migration5to6OnAnEmptyDatabaseStillAddsTheThreeSavingsDefaults() = runBlocking {
        val name = "migration-savings-empty.db"
        createV5(name) { }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7).allowMainThreadQueries().build()
        try {
            assertEquals(0, db.transactionDao().getAllOnce().size)
            assertEquals(3, db.categoryDao().getAllOnce().count { it.type == FlowType.SAVINGS })
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun v6EntitiesRoundTripSavingsAndForecasts() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val day = LocalDate.of(2026, 9, 27)
            val savingsCat = CategoryEntity(name = "Goals", iconKey = "coin", type = FlowType.SAVINGS, sortOrder = 0)
            val catId = db.categoryDao().insert(savingsCat)

            db.transactionDao().insert(
                TransactionEntity(
                    amount = 5000.0, description = "", categoryId = catId, date = day, createdAt = 1L,
                    type = FlowType.SAVINGS, contributionKind = com.ajesh.syncspend.domain.model.ContributionKind.TRANSFER,
                ),
            )
            val savedTx = db.transactionDao().getAllOnce().single()
            assertEquals(FlowType.SAVINGS, savedTx.type)
            assertEquals(com.ajesh.syncspend.domain.model.ContributionKind.TRANSFER, savedTx.contributionKind)
            assertNull(savedTx.fundingSource)

            db.forecastDao().insert(com.ajesh.syncspend.data.db.entity.ForecastEntity(note = "Rent", amount = 12000.0, date = null))
            db.forecastDao().insert(com.ajesh.syncspend.data.db.entity.ForecastEntity(note = "Trip", amount = 8000.0, date = day))
            val forecasts = db.forecastDao().getAllOnce()
            assertEquals(2, forecasts.size)
            assertNull(forecasts.single { it.note == "Rent" }.date)
            assertEquals(day, forecasts.single { it.note == "Trip" }.date)
        } finally {
            db.close()
        }
    }

    @Test fun v4EntitiesRoundTrip() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val day = LocalDate.of(2026, 9, 27)
            db.transactionDao().insert(TransactionEntity(amount = -250.0, description = "with a note", categoryId = 1, date = day, createdAt = 1L, timeMinuteOfDay = 875))
            db.transactionDao().insert(TransactionEntity(amount = -90.0, description = "", categoryId = 1, date = day, createdAt = 2L))
            val saved = db.transactionDao().getAllOnce().sortedBy { it.id }
            assertEquals(875, saved[0].timeMinuteOfDay)
            assertNull(saved[1].timeMinuteOfDay)

            db.subscriptionDao().insert(SubscriptionEntity(name = "Gym", iconKey = "dumbbell", amount = 1200.0, billingCycle = BillingCycle.MONTHLY, nextDueDate = day, categoryId = null, active = true, remindDaysBefore = "3,7"))
            db.subscriptionDao().insert(SubscriptionEntity(name = "Cloud", iconKey = "repeat", amount = 99.0, billingCycle = BillingCycle.MONTHLY, nextDueDate = day, categoryId = null, active = true))
            val subs = db.subscriptionDao().getActiveOnce().associateBy { it.name }
            assertEquals("3,7", subs.getValue("Gym").remindDaysBefore)
            assertEquals("1,3", subs.getValue("Cloud").remindDaysBefore)
        } finally {
            db.close()
        }
    }
}
