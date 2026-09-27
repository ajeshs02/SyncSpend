package com.ajesh.syncspend

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.ajesh.syncspend.data.db.AppDatabase
import com.ajesh.syncspend.data.db.MIGRATION_3_4
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.model.BillingCycle
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
            .addMigrations(MIGRATION_3_4).allowMainThreadQueries().build()
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
            .addMigrations(MIGRATION_3_4).allowMainThreadQueries().build()
        try {
            assertEquals(0, db.transactionDao().getAllOnce().size)
        } finally {
            db.close()
            context.deleteDatabase(name)
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
