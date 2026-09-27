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
