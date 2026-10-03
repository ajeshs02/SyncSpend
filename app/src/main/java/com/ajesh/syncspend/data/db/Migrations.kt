package com.ajesh.syncspend.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * v3 -> v4 (Room runs a migration inside one transaction):
 *  - `transactions.timeMinuteOfDay` (nullable): when the entry happened. Backfilled only for rows
 *    whose `createdAt` falls on the entry's own date, the one case where the logging time is the
 *    entry's time; back-dated and imported rows stay null.
 *  - `subscriptions.remindDaysBefore`: the "remind me N days before" set, "1,3" by default.
 *  - A transaction's description used to be auto-filled with its category name. It is now an
 *    optional note, so those copies are blanked; real notes are kept.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) = migrate3To4(db, ZoneId.systemDefault())
}

/** v4 -> v5: `subscriptions.remindMinuteOfDay`, a per-subscription reminder time (9:00 AM by default, matching the old hardcoded time). */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `remindMinuteOfDay` INTEGER NOT NULL DEFAULT 540")
    }
}

/** v6 -> v7: the `forecasts` table for the Home "Forecast" tile — a plain note-taking feature, no columns to backfill. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `forecasts` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`note` TEXT NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`date` INTEGER)",
        )
    }
}

/**
 * v5 -> v6: introduces Savings as a third [com.ajesh.syncspend.domain.model.FlowType].
 *  - `transactions.type`: was derived from the amount's sign (can no longer tell Income apart from
 *    a Savings contribution, both positive) — now a real stored column, backfilled here with the
 *    exact old sign rule (`amount >= 0 -> INCOME else EXPENSE`) for every existing row, so nothing
 *    existing changes meaning. SQLite's `ALTER TABLE ADD COLUMN` can't reference another column in
 *    its default, hence the add-then-update two-step.
 *  - `transactions.fundingSource` / `transactions.contributionKind`: new, nullable, untouched for
 *    every pre-existing row (null means "regular funds" / "new income," the ordinary case).
 *  - Three new default Savings categories, inserted directly (not through the app's normal
 *    [com.ajesh.syncspend.data.repository.CategorySeeder], which only ever runs once per install and
 *    has already run for every existing user) so upgraders get them too, not just fresh installs.
 *    The pre-existing Expense category literally named "Savings" is deliberately left untouched.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `type` TEXT NOT NULL DEFAULT 'EXPENSE'")
        db.execSQL("UPDATE `transactions` SET `type` = 'INCOME' WHERE `amount` >= 0")
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `fundingSource` TEXT")
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `contributionKind` TEXT")

        val nextOrder = db.query("SELECT COALESCE(MAX(`sortOrder`), -1) + 1 FROM `categories` WHERE `type` = 'SAVINGS'").use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        val insert = db.compileStatement(
            "INSERT INTO `categories` (`name`, `iconKey`, `type`, `sortOrder`, `archived`) VALUES (?, ?, 'SAVINGS', ?, 0)",
        )
        listOf("Savings Goals" to "coin", "Emergency Fund" to "shield", "Investments" to "trend")
            .forEachIndexed { index, (name, icon) ->
                insert.bindString(1, name)
                insert.bindString(2, icon)
                insert.bindLong(3, (nextOrder + index).toLong())
                insert.executeInsert()
            }
        insert.close()
    }
}

/**
 * v7 -> v8: replaces Savings-as-a-third-[com.ajesh.syncspend.domain.model.FlowType] with the separate
 * Transfer ledger.
 *  - `transfers`: the new table (see [com.ajesh.syncspend.data.db.entity.TransferEntity]) — independent
 *    of `transactions`, so Transfers structurally cannot inflate Income/Expense totals.
 *  - `forecasts.categoryId` / `completed` / `completedTransactionId`: the Forecast "mark done" flow
 *    (see [com.ajesh.syncspend.data.db.entity.ForecastEntity]'s doc).
 *  - The three `FlowType.SAVINGS` categories `MIGRATION_5_6` inserted ("Savings Goals", "Emergency
 *    Fund", "Investments") are deleted outright, not archived: nothing has ever referenced them (no
 *    Savings transaction was ever created against this schema), and `FlowType` no longer has a
 *    `SAVINGS` value for `Converters` to deserialize, so a leftover `type='SAVINGS'` row — archived or
 *    not — would crash the first time it's read. Deleting is safe and avoids that entirely.
 *  - Seeds the Income categories "Add to Savings" picks a source from (PF, Reward, Previous Savings,
 *    Other — "Salary"/"Freelance"/"Gift" already exist), skipping any that already exist by
 *    (type, lowercase name), the same rule [com.ajesh.syncspend.domain.model.DefaultCategories.missingFrom] uses.
 *  - `transactions.fundingSource`/`contributionKind` are deliberately left in place, unused — see
 *    [com.ajesh.syncspend.data.db.entity.TransactionEntity]'s doc.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `transfers` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`direction` TEXT NOT NULL, " +
                "`date` INTEGER NOT NULL, " +
                "`categoryId` INTEGER, " +
                "`note` TEXT NOT NULL DEFAULT '', " +
                "`createdAt` INTEGER NOT NULL, " +
                "`timeMinuteOfDay` INTEGER)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_date` ON `transfers` (`date`)")

        db.execSQL("ALTER TABLE `forecasts` ADD COLUMN `categoryId` INTEGER")
        db.execSQL("ALTER TABLE `forecasts` ADD COLUMN `completed` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `forecasts` ADD COLUMN `completedTransactionId` INTEGER")

        db.execSQL(
            "DELETE FROM `categories` WHERE `type` = 'SAVINGS' AND `name` IN " +
                "('Savings Goals', 'Emergency Fund', 'Investments')",
        )

        val existingIncomeNames = HashSet<String>().apply {
            db.query("SELECT `name` FROM `categories` WHERE `type` = 'INCOME'").use { c ->
                val col = c.getColumnIndex("name")
                while (c.moveToNext()) add(c.getString(col).trim().lowercase())
            }
        }
        var nextOrder = db.query("SELECT COALESCE(MAX(`sortOrder`), -1) + 1 FROM `categories` WHERE `type` = 'INCOME'").use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        val insert = db.compileStatement(
            "INSERT INTO `categories` (`name`, `iconKey`, `type`, `sortOrder`, `archived`) VALUES (?, ?, 'INCOME', ?, 0)",
        )
        listOf("PF" to "shield", "Reward" to "spark", "Previous Savings" to "wallet", "Other" to "tag")
            .filter { (name, _) -> name.lowercase() !in existingIncomeNames }
            .forEach { (name, icon) ->
                insert.bindString(1, name)
                insert.bindString(2, icon)
                insert.bindLong(3, nextOrder.toLong())
                insert.executeInsert()
                nextOrder++
            }
        insert.close()
    }
}

internal fun migrate3To4(db: SupportSQLiteDatabase, zone: ZoneId) {
    db.execSQL("ALTER TABLE `transactions` ADD COLUMN `timeMinuteOfDay` INTEGER")
    db.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `remindDaysBefore` TEXT NOT NULL DEFAULT '1,3'")
    db.execSQL(
        "UPDATE `transactions` SET `description` = '' WHERE `description` = " +
            "(SELECT `name` FROM `categories` WHERE `categories`.`id` = `transactions`.`categoryId`)",
    )

    val updates = ArrayList<Pair<Long, Int>>()
    db.query("SELECT `id`, `date`, `createdAt` FROM `transactions`").use { c ->
        while (c.moveToNext()) {
            val created = Instant.ofEpochMilli(c.getLong(2)).atZone(zone)
            if (created.toLocalDate() == LocalDate.ofEpochDay(c.getLong(1))) {
                updates += c.getLong(0) to created.hour * 60 + created.minute
            }
        }
    }
    if (updates.isNotEmpty()) {
        val statement = db.compileStatement("UPDATE `transactions` SET `timeMinuteOfDay` = ? WHERE `id` = ?")
        updates.forEach { (id, minute) ->
            statement.bindLong(1, minute.toLong())
            statement.bindLong(2, id)
            statement.executeUpdateDelete()
        }
        statement.close()
    }
}
