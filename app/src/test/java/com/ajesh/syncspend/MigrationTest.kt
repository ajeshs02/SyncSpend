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
import com.ajesh.syncspend.data.db.MIGRATION_7_8
import com.ajesh.syncspend.data.db.MIGRATION_8_9
import com.ajesh.syncspend.data.db.MIGRATION_9_10
import com.ajesh.syncspend.data.db.MIGRATION_10_11
import com.ajesh.syncspend.data.db.MIGRATION_11_12
import com.ajesh.syncspend.data.db.MIGRATION_12_13
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.ForecastEntity
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
private val ALL_MIGRATIONS = arrayOf(
    MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
    MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
)

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

    /**
     * Covers the full v5 -> v10 chain: MIGRATION_5_6's type backfill and its (now-historical) 3
     * Savings categories, which MIGRATION_7_8 deletes again in the same upgrade run; the pre-existing
     * Expense category literally named "Savings" (and any transaction filed under it) is now itself
     * deleted by MIGRATION_8_9 — reversing MIGRATION_7_8's original preservation of that exact category,
     * per the user's explicit instruction to remove it outright, used or not (see MIGRATION_8_9's doc).
     */
    @Test fun migration5to10BackfillsTypesThenDeletesOldSavingsCategoryAndSeedsIncomeSources() = runBlocking {
        val name = "migration-savings.db"
        val day = LocalDate.of(2026, 9, 20)
        createV5(name) { db ->
            db.execSQL("INSERT INTO categories VALUES (1, 'Food', 'utensils', 'EXPENSE', 0, 0)")
            db.execSQL("INSERT INTO categories VALUES (2, 'Salary', 'bank', 'INCOME', 0, 0)")
            // The pre-existing Expense category literally named "Savings" — and a transaction filed
            // under it — must both be gone after this chain, not preserved.
            db.execSQL("INSERT INTO categories VALUES (3, 'Savings', 'coin', 'EXPENSE', 6, 0)")
            db.execSQL("INSERT INTO transactions (id, amount, description, categoryId, date, createdAt, timeMinuteOfDay) VALUES (1, -250.0, '', 1, ${day.toEpochDay()}, 1, NULL)")
            db.execSQL("INSERT INTO transactions (id, amount, description, categoryId, date, createdAt, timeMinuteOfDay) VALUES (2, 50000.0, '', 2, ${day.toEpochDay()}, 2, NULL)")
            db.execSQL("INSERT INTO transactions (id, amount, description, categoryId, date, createdAt, timeMinuteOfDay) VALUES (3, -500.0, '', 3, ${day.toEpochDay()}, 3, NULL)")
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(
                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
            ).allowMainThreadQueries().build()
        try {
            val rows = db.transactionDao().getAllOnce().associateBy { it.id }
            assertEquals(FlowType.EXPENSE, rows.getValue(1).type)
            assertEquals(FlowType.INCOME, rows.getValue(2).type)
            // The transaction filed under the deleted "Savings" Expense category is gone too.
            assertTrue(3L !in rows.keys)
            assertEquals(2, rows.size)

            val categories = db.categoryDao().getAllOnce()
            assertTrue(categories.none { it.name == "Savings" && it.type == FlowType.EXPENSE })

            // The 3 categories MIGRATION_5_6 inserted for the now-removed Savings FlowType are gone —
            // MIGRATION_7_8 deletes them in the same upgrade chain.
            assertTrue(categories.none { it.name in setOf("Savings Goals", "Emergency Fund", "Investments") })

            // The new suggested transfer-source Income categories were seeded (this fixture had no
            // Freelance/Gift/etc. to begin with — only the 4 new ones MIGRATION_7_8 itself inserts).
            val incomeNames = categories.filter { it.type == FlowType.INCOME }.map { it.name }.toSet()
            assertEquals(setOf("Salary", "PF", "Reward", "Previous Savings", "Other"), incomeNames)

            // MIGRATION_6_7 created the forecasts table in the same upgrade chain — usable immediately.
            // (MIGRATION_7_8 also created `transfers`, but MIGRATION_11_12 later in this same chain
            // drops it again — the Savings & Investment feature's removal — so there's nothing to
            // exercise here anymore.)
            val forecastId = db.forecastDao().insert(ForecastEntity(note = "Rent", amount = 12000.0, date = null))
            assertEquals(1, db.forecastDao().getAllOnce().filter { it.id == forecastId }.size)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun migration9to10BackfillsPreExistingTransfersAsSavings() = runBlocking {
        val name = "migration-transfer-kind.db"
        val day = LocalDate.of(2026, 9, 20)
        createV5(name) { }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(
                object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            ).build(),
        )
        val raw = helper.writableDatabase
        // Run the chain by hand only up to v8 (raw SQL, bypassing Room's own bookkeeping) so a transfer
        // can be inserted the way a real pre-Investment row would look — with no `kind` column yet, since
        // that column doesn't exist until MIGRATION_9_10. The pragma is left at the real intermediate
        // version (8), not jumped ahead, so Room can still run (and correctly account for) the v8 -> v10
        // leg itself below.
        MIGRATION_5_6.migrate(raw)
        MIGRATION_6_7.migrate(raw)
        MIGRATION_7_8.migrate(raw)
        raw.execSQL(
            "INSERT INTO transfers (id, amount, direction, date, categoryId, note, createdAt, timeMinuteOfDay) " +
                "VALUES (1, 500.0, 'TO_SAVINGS', ${day.toEpochDay()}, NULL, '', 1, NULL)",
        )
        // Run the next leg (the `kind` backfill itself) by hand too, via raw SQL rather than through
        // TransferDao — the Savings & Investment feature (and TransferDao with it) is removed later in
        // this same version chain, so this test no longer has a live DAO to read the row back through.
        MIGRATION_8_9.migrate(raw)
        MIGRATION_9_10.migrate(raw)
        raw.query("SELECT kind FROM transfers WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("SAVINGS", c.getString(0))
        }
        raw.version = 10
        helper.close()

        // Confirm the rest of the chain — including the Savings & Investment removal two versions
        // later — still builds cleanly from here.
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13).allowMainThreadQueries().build()
        try {
            assertEquals(0, db.transactionDao().getAllOnce().size)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun migration10to11BackfillsForecastDescriptionAsBlank() = runBlocking {
        val name = "migration-forecast-description.db"
        createV5(name) { }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(
                object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            ).build(),
        )
        val raw = helper.writableDatabase
        // Same technique as the transfer-kind backfill test above: run the chain by hand only up to v10
        // (no `description` column yet), insert a forecast the way a real pre-note row would look, then
        // let Room run the v10 -> v11 leg itself.
        MIGRATION_5_6.migrate(raw)
        MIGRATION_6_7.migrate(raw)
        MIGRATION_7_8.migrate(raw)
        MIGRATION_8_9.migrate(raw)
        MIGRATION_9_10.migrate(raw)
        raw.execSQL("INSERT INTO forecasts (id, note, amount, date, categoryId, completed, completedTransactionId) VALUES (1, 'Rent', 12000.0, NULL, NULL, 0, NULL)")
        raw.version = 10
        helper.close()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13).allowMainThreadQueries().build()
        try {
            val forecast = db.forecastDao().getAllOnce().single()
            assertEquals("", forecast.description)
            assertEquals("Rent", forecast.note)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    /**
     * The actual Savings & Investment removal: a populated v11 fixture (a real `transfers` row, plus a
     * transaction still carrying the legacy `fundingSource`/`contributionKind` values) migrated through
     * both new versions should end up with the `transfers` table gone, the two legacy columns gone from
     * `transactions`, and every other column's data on that transaction untouched.
     */
    @Test fun migration11to13DropsTransfersAndLegacyColumnsPreservingOtherData() = runBlocking {
        val name = "migration-drop-transfers-legacy.db"
        val day = LocalDate.of(2026, 9, 20)
        createV5(name) { }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(
                object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            ).build(),
        )
        val raw = helper.writableDatabase
        MIGRATION_5_6.migrate(raw)
        MIGRATION_6_7.migrate(raw)
        MIGRATION_7_8.migrate(raw)
        MIGRATION_8_9.migrate(raw)
        MIGRATION_9_10.migrate(raw)
        MIGRATION_10_11.migrate(raw)
        raw.execSQL("INSERT INTO categories VALUES (1, 'Food', 'utensils', 'EXPENSE', 0, 0)")
        raw.execSQL(
            "INSERT INTO transactions (id, amount, description, categoryId, date, createdAt, timeMinuteOfDay, type, fundingSource, contributionKind) " +
                "VALUES (1, -100.0, 'Lunch', 1, ${day.toEpochDay()}, 1, NULL, 'EXPENSE', 'SAVINGS', 'TRANSFER')",
        )
        raw.execSQL(
            "INSERT INTO transfers (id, amount, direction, date, categoryId, note, createdAt, timeMinuteOfDay, kind) " +
                "VALUES (1, 500.0, 'TO_SAVINGS', ${day.toEpochDay()}, NULL, '', 1, NULL, 'SAVINGS')",
        )
        raw.version = 11
        helper.close()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_11_12, MIGRATION_12_13).allowMainThreadQueries().build()
        try {
            // The `transfers` table itself is gone.
            db.openHelper.writableDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name='transfers'").use { c ->
                assertEquals(0, c.count)
            }

            // `fundingSource`/`contributionKind` are gone from `transactions`'s own schema.
            val columnNames = mutableSetOf<String>()
            db.openHelper.writableDatabase.query("PRAGMA table_info(`transactions`)").use { c ->
                val nameIdx = c.getColumnIndex("name")
                while (c.moveToNext()) columnNames += c.getString(nameIdx)
            }
            assertTrue("fundingSource" !in columnNames)
            assertTrue("contributionKind" !in columnNames)

            // Every other column on the surviving transaction is untouched by the rebuild.
            val row = db.transactionDao().getAllOnce().single()
            assertEquals(-100.0, row.amount, 0.0)
            assertEquals("Lunch", row.description)
            assertEquals(1L, row.categoryId)
            assertEquals(day, row.date)
            assertEquals(1L, row.createdAt)
            assertNull(row.timeMinuteOfDay)
            assertEquals(FlowType.EXPENSE, row.type)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun migration5to8OnAnEmptyDatabaseSeedsIncomeSourcesAndNoSavingsCategories() = runBlocking {
        val name = "migration-savings-empty.db"
        createV5(name) { }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(
                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
            ).allowMainThreadQueries().build()
        try {
            assertEquals(0, db.transactionDao().getAllOnce().size)
            val categories = db.categoryDao().getAllOnce()
            assertTrue(categories.none { it.name in setOf("Savings Goals", "Emergency Fund", "Investments") })
            assertEquals(setOf("PF", "Reward", "Previous Savings", "Other"), categories.filter { it.type == FlowType.INCOME }.map { it.name }.toSet())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun v8EntitiesRoundTripForecasts() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val day = LocalDate.of(2026, 9, 27)

            val forecastId = db.forecastDao().insert(ForecastEntity(note = "Rent", amount = 12000.0, date = null))
            db.forecastDao().insert(ForecastEntity(note = "Trip", amount = 8000.0, date = day))
            val forecasts = db.forecastDao().getAllOnce()
            assertEquals(2, forecasts.size)
            assertNull(forecasts.single { it.note == "Rent" }.date)
            assertEquals(day, forecasts.single { it.note == "Trip" }.date)

            // Mark-done-and-add-expense's completedTransactionId link, and its clearing on delete.
            val expenseCat = db.categoryDao().insert(CategoryEntity(name = "Food", iconKey = "utensils", type = FlowType.EXPENSE, sortOrder = 0))
            val txId = db.transactionDao().insert(TransactionEntity(amount = -8000.0, description = "", categoryId = expenseCat, date = day, createdAt = 2L, type = FlowType.EXPENSE))
            db.forecastDao().update(forecasts.single { it.note == "Trip" }.copy(completed = true, completedTransactionId = txId))
            assertEquals(txId, db.forecastDao().getAllOnce().single { it.note == "Trip" }.completedTransactionId)
            db.forecastDao().clearCompletedLink(txId)
            val afterClear = db.forecastDao().getAllOnce().single { it.note == "Trip" }
            assertNull(afterClear.completedTransactionId)
            assertTrue(afterClear.completed) // completed stays true even though the link is gone
        } finally {
            db.close()
        }
    }

    /**
     * Round 8's Forecast refinements at the DAO level (no ViewModel/coroutine risk — see this
     * project's documented Robolectric main-dispatcher hang for why ViewModel-level tests are
     * avoided here): Mark Done / Mark Undone toggle the flag with no transaction created either way;
     * the past-month review's bulk "Mark completed" preserves every row (never deletes); and a
     * forecast linked to a real expense keeps that link (and `completed`) independent of edits to
     * its own note/amount/category/date fields.
     */
    @Test fun forecastMarkDoneUndoAndBulkReviewPreserveRows() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val day = LocalDate.of(2026, 9, 27)
            val id = db.forecastDao().insert(ForecastEntity(note = "Rent", amount = 12000.0, date = day))
            var row = db.forecastDao().getAllOnce().single { it.id == id }
            assertTrue(!row.completed)

            // Mark Done (no linked expense): completed flips true, no transaction, no link.
            db.forecastDao().update(row.copy(completed = true))
            row = db.forecastDao().getAllOnce().single { it.id == id }
            assertTrue(row.completed)
            assertNull(row.completedTransactionId)

            // Mark Undone reverses it — only ever offered when there's no link, exactly this state.
            db.forecastDao().update(row.copy(completed = false))
            row = db.forecastDao().getAllOnce().single { it.id == id }
            assertTrue(!row.completed)

            // Editing the forecast's own fields never touches a linked expense (there is none here) —
            // and, separately, a stale last-month pending forecast reviewed via "Mark completed" is
            // updated in place, never deleted.
            val staleId = db.forecastDao().insert(ForecastEntity(note = "Old plan", amount = 500.0, date = day.minusMonths(1)))
            db.forecastDao().markCompleted(listOf(staleId))
            val stale = db.forecastDao().getAllOnce().single { it.id == staleId }
            assertTrue(stale.completed)
            assertEquals("Old plan", stale.note) // untouched — only `completed` changed
            assertEquals(2, db.forecastDao().getAllOnce().size) // both rows preserved, nothing deleted
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
