package com.ajesh.syncspend.csv

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.ajesh.syncspend.data.db.AppDatabase
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.ForecastEntity
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.data.db.entity.TransferEntity
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ReminderSchedule
import com.ajesh.syncspend.domain.model.TransferDirection
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BundleImportResult(
    val importedTransactions: Int = 0,
    val importedReminders: Int = 0,
    val importedSubscriptions: Int = 0,
    val importedForecasts: Int = 0,
    val importedTransfers: Int = 0,
    val createdCategories: Int = 0,
    val error: String? = null,
)

/**
 * Imports the bundle [BundleExporter] writes. Hardened per the round's explicit ask:
 *  - **Parse everything before writing anything.** Every section present in the file is parsed into
 *    plain rows first; if any section is malformed, [import] returns an error and the database is
 *    never touched — no partial import.
 *  - **One atomic transaction.** The actual writes for every section present happen inside a single
 *    `db.withTransaction`, so a failure partway through rolls back every table it touched, not just
 *    the one that failed.
 *  - **Backward compatible.** A section key that's absent is left untouched (nothing deleted,
 *    nothing changed) — a bundle exported before this round, with `fundingSource`/`contributionKind`
 *    still present on its transactions or no `transfers` key at all, still imports cleanly (the old
 *    fields are simply never read; a missing `transfers` key just skips that section).
 *  - **Never reuses source ids.** Every row is inserted fresh; a transaction's or transfer's category
 *    is resolved by (name, type) — exactly [CsvImporter]'s own rule — never by a numeric id from the
 *    file. A Forecast's [ForecastEntity.completedTransactionId] is never round-tripped for the same
 *    reason (a cross-table id has no portable meaning across an export/import boundary) — an imported
 *    completed forecast simply has no linked transaction, same as the legitimate "completed, not yet
 *    linked" state `ForecastViewModel` already supports.
 *  - **Duplicate rule is per-entity and exact-match only** (see each section's block below) — skip an
 *    identical row, otherwise insert as new. There is no overwrite-merge path anywhere here.
 */
object BundleImporter {

    private class ParsedBundle(
        val transactions: List<ParsedTx>?,
        val reminders: List<ReminderEntity>?,
        val subscriptions: List<ParsedSub>?,
        val forecasts: List<ParsedForecast>?,
        val transfers: List<ParsedTransfer>?,
    )

    private class ParsedTx(
        val date: LocalDate, val type: FlowType, val category: String, val description: String, val amount: Double,
    )

    private class ParsedSub(
        val name: String, val iconKey: String, val amount: Double, val billingCycle: BillingCycle,
        val nextDueDate: LocalDate, val categoryName: String?, val categoryType: FlowType?,
        val active: Boolean, val remindDaysBefore: String, val remindMinuteOfDay: Int,
    )

    private class ParsedForecast(val note: String, val amount: Double, val date: LocalDate?, val categoryName: String?, val completed: Boolean)

    private class ParsedTransfer(
        val date: LocalDate, val direction: TransferDirection, val categoryName: String?, val note: String, val amount: Double,
    )

    suspend fun import(context: Context, uri: Uri, db: AppDatabase): BundleImportResult = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext BundleImportResult(error = "Could not open the file.")
        val root = try {
            JSONObject(String(bytes, Charsets.UTF_8).removePrefix("﻿"))
        } catch (e: Exception) {
            return@withContext BundleImportResult(error = "This isn't a valid backup file.")
        }

        val parsed = try {
            ParsedBundle(
                transactions = root.optJSONArray("transactions")?.let(::parseTransactions),
                reminders = root.optJSONArray("reminders")?.let(::parseReminders),
                subscriptions = root.optJSONArray("subscriptions")?.let(::parseSubscriptions),
                forecasts = root.optJSONArray("forecasts")?.let(::parseForecasts),
                transfers = root.optJSONArray("transfers")?.let(::parseTransfers),
            )
        } catch (e: Exception) {
            return@withContext BundleImportResult(error = "The backup file is damaged, so nothing was imported.")
        }

        var importedTx = 0
        var importedReminders = 0
        var importedSubs = 0
        var importedForecasts = 0
        var importedTransfers = 0
        var created = 0

        db.withTransaction {
            val categoryDao = db.categoryDao()
            val byKey = HashMap<Pair<FlowType, String>, CategoryEntity>()
            val nextOrder = HashMap<FlowType, Int>()
            categoryDao.getAllOnce().forEach { c ->
                byKey.putIfAbsent(c.type to c.name.trim().lowercase(), c)
                nextOrder[c.type] = maxOf(nextOrder[c.type] ?: 0, c.sortOrder + 1)
            }

            suspend fun resolveCategory(name: String, type: FlowType): CategoryEntity {
                val key = type to name.trim().lowercase()
                byKey[key]?.let { existing ->
                    if (!existing.archived) return existing
                    val revived = existing.copy(archived = false)
                    categoryDao.update(revived)
                    byKey[key] = revived
                    return revived
                }
                val fresh = CategoryEntity(
                    name = name.trim(), iconKey = CategoryIconGuesser.guess(name), type = type, sortOrder = nextOrder[type] ?: 0,
                )
                nextOrder[type] = fresh.sortOrder + 1
                val saved = fresh.copy(id = categoryDao.insert(fresh))
                byKey[key] = saved
                created++
                return saved
            }

            parsed.transactions?.let { rows ->
                // Same dedupe rule CsvImporter already uses for this exact row shape.
                val dupTracker = DuplicateTracker(db.transactionDao().getAllOnce())
                val now = System.currentTimeMillis()
                val toInsert = ArrayList<TransactionEntity>()
                rows.forEachIndexed { index, row ->
                    val category = resolveCategory(row.category, row.type)
                    if (!dupTracker.consumeIfDuplicate(row.date, row.amount, category.id, row.description)) {
                        toInsert += TransactionEntity(
                            amount = row.amount,
                            description = row.description,
                            categoryId = category.id,
                            date = row.date,
                            createdAt = now + index,
                            type = row.type,
                        )
                    }
                }
                db.transactionDao().insertAll(toInsert)
                importedTx = toInsert.size
            }

            parsed.reminders?.let { rows ->
                val reminderDao = db.reminderDao()
                // Duplicate key: identical label + schedule + time + next trigger date.
                val existingKeys = reminderDao.getAllOnce()
                    .mapTo(HashSet()) { listOf(it.label.trim().lowercase(), it.schedule, it.timeMinuteOfDay, it.nextTriggerDate) }
                rows.forEach { r ->
                    val key = listOf(r.label.trim().lowercase(), r.schedule, r.timeMinuteOfDay, r.nextTriggerDate)
                    if (key !in existingKeys) {
                        reminderDao.insert(r)
                        importedReminders++
                    }
                }
            }

            parsed.subscriptions?.let { rows ->
                val subscriptionDao = db.subscriptionDao()
                val existingKeys = subscriptionDao.getAllOnce()
                    .mapTo(HashSet()) { listOf(it.name.trim().lowercase(), it.amount, it.billingCycle, it.nextDueDate) }
                rows.forEach { row ->
                    val key = listOf(row.name.trim().lowercase(), row.amount, row.billingCycle, row.nextDueDate)
                    if (key !in existingKeys) {
                        val categoryId = row.categoryName?.let { name -> resolveCategory(name, row.categoryType ?: FlowType.EXPENSE).id }
                        subscriptionDao.insert(
                            SubscriptionEntity(
                                name = row.name,
                                iconKey = row.iconKey,
                                amount = row.amount,
                                billingCycle = row.billingCycle,
                                nextDueDate = row.nextDueDate,
                                categoryId = categoryId,
                                active = row.active,
                                remindDaysBefore = row.remindDaysBefore,
                                remindMinuteOfDay = row.remindMinuteOfDay,
                            ),
                        )
                        importedSubs++
                    }
                }
            }

            parsed.forecasts?.let { rows ->
                val forecastDao = db.forecastDao()
                val existingKeys = forecastDao.getAllOnce().mapTo(HashSet()) { listOf(it.note.trim().lowercase(), it.amount, it.date) }
                rows.forEach { f ->
                    val key = listOf(f.note.trim().lowercase(), f.amount, f.date)
                    if (key !in existingKeys) {
                        val categoryId = f.categoryName?.let { name -> resolveCategory(name, FlowType.EXPENSE).id }
                        forecastDao.insert(ForecastEntity(note = f.note, amount = f.amount, date = f.date, categoryId = categoryId, completed = f.completed))
                        importedForecasts++
                    }
                }
            }

            parsed.transfers?.let { rows ->
                val transferDao = db.transferDao()
                val existingKeys = transferDao.getAllOnce()
                    .mapTo(HashSet()) { listOf(it.date, (it.amount * 100).toLong(), it.direction, it.categoryId, it.note.trim().lowercase()) }
                val now = System.currentTimeMillis()
                rows.forEachIndexed { index, row ->
                    val categoryId = row.categoryName?.let { name -> resolveCategory(name, FlowType.INCOME).id }
                    val key = listOf(row.date, (row.amount * 100).toLong(), row.direction, categoryId, row.note.trim().lowercase())
                    if (key !in existingKeys) {
                        transferDao.insert(
                            TransferEntity(
                                amount = row.amount,
                                direction = row.direction,
                                date = row.date,
                                categoryId = categoryId,
                                note = row.note,
                                createdAt = now + index,
                            ),
                        )
                        importedTransfers++
                    }
                }
            }
        }

        BundleImportResult(importedTx, importedReminders, importedSubs, importedForecasts, importedTransfers, created)
    }

    private fun parseTransactions(arr: JSONArray): List<ParsedTx> = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        ParsedTx(
            date = LocalDate.parse(o.getString("date")),
            // Missing/unknown "type" falls back to the pre-Savings sign rule, so an older export always round-trips.
            // A legacy "SAVINGS" type string (from a bundle exported during the brief Savings-as-a-third-flow
            // round) also falls back to this rule, same as an unrecognized value would.
            type = o.optString("type", "").let { t -> runCatching { FlowType.valueOf(t) }.getOrNull() }
                ?: if (o.getDouble("amount") >= 0) FlowType.INCOME else FlowType.EXPENSE,
            category = o.optString("category", CsvImportParser.UNCATEGORIZED).ifBlank { CsvImportParser.UNCATEGORIZED },
            description = o.optString("description", ""),
            amount = o.getDouble("amount"),
        )
    }

    private fun parseReminders(arr: JSONArray): List<ReminderEntity> = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        ReminderEntity(
            label = o.getString("label"),
            iconKey = o.optString("iconKey", "bell"),
            schedule = ReminderSchedule.valueOf(o.getString("schedule")),
            timeMinuteOfDay = o.getInt("timeMinuteOfDay"),
            nextTriggerDate = LocalDate.parse(o.getString("nextTriggerDate")),
            active = o.optBoolean("active", true),
        )
    }

    private fun parseSubscriptions(arr: JSONArray): List<ParsedSub> = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        ParsedSub(
            name = o.getString("name"),
            iconKey = o.optString("iconKey", "repeat"),
            amount = o.getDouble("amount"),
            billingCycle = BillingCycle.valueOf(o.getString("billingCycle")),
            nextDueDate = LocalDate.parse(o.getString("nextDueDate")),
            categoryName = o.optString("category", "").takeIf { it.isNotBlank() },
            categoryType = o.optString("categoryType", "").takeIf { it.isNotBlank() }?.let { FlowType.valueOf(it) },
            active = o.optBoolean("active", true),
            remindDaysBefore = o.optString("remindDaysBefore", "1,3"),
            remindMinuteOfDay = o.optInt("remindMinuteOfDay", 540),
        )
    }

    private fun parseForecasts(arr: JSONArray): List<ParsedForecast> = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        ParsedForecast(
            note = o.getString("note"),
            amount = o.getDouble("amount"),
            date = o.optString("date", "").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
            categoryName = o.optString("category", "").takeIf { it.isNotBlank() },
            completed = o.optBoolean("completed", false),
        )
    }

    private fun parseTransfers(arr: JSONArray): List<ParsedTransfer> = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        ParsedTransfer(
            date = LocalDate.parse(o.getString("date")),
            direction = TransferDirection.valueOf(o.getString("direction")),
            categoryName = o.optString("category", "").takeIf { it.isNotBlank() },
            note = o.optString("note", ""),
            amount = o.getDouble("amount"),
        )
    }
}
