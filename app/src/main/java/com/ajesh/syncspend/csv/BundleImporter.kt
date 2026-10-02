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
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.ContributionKind
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.FundingSource
import com.ajesh.syncspend.domain.model.ReminderSchedule
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
 *    nothing changed) — a bundle exported before Savings existed, with no `fundingSource`/
 *    `contributionKind` fields, still imports cleanly (both read as null via `opt...`).
 *  - **Never reuses source ids.** Every row is inserted fresh; a transaction's category is resolved
 *    by (name, type) — exactly [CsvImporter]'s own rule — never by a numeric id from the file, which
 *    could collide with an unrelated local row.
 *  - **Duplicate rule is per-entity and exact-match only** (see each section's block below) — skip an
 *    identical row, otherwise insert as new. There is no overwrite-merge path anywhere here.
 */
object BundleImporter {

    private class ParsedBundle(
        val transactions: List<ParsedTx>?,
        val reminders: List<ReminderEntity>?,
        val subscriptions: List<ParsedSub>?,
        val forecasts: List<ForecastEntity>?,
    )

    private class ParsedTx(
        val date: LocalDate, val type: FlowType, val category: String, val description: String,
        val amount: Double, val fundingSource: FundingSource?, val contributionKind: ContributionKind?,
    )

    private class ParsedSub(
        val name: String, val iconKey: String, val amount: Double, val billingCycle: BillingCycle,
        val nextDueDate: LocalDate, val categoryName: String?, val categoryType: FlowType?,
        val active: Boolean, val remindDaysBefore: String, val remindMinuteOfDay: Int,
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
            )
        } catch (e: Exception) {
            return@withContext BundleImportResult(error = "The backup file is damaged — nothing was imported.")
        }

        var importedTx = 0
        var importedReminders = 0
        var importedSubs = 0
        var importedForecasts = 0
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
                            fundingSource = row.fundingSource,
                            contributionKind = row.contributionKind,
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
                        forecastDao.insert(f)
                        importedForecasts++
                    }
                }
            }
        }

        BundleImportResult(importedTx, importedReminders, importedSubs, importedForecasts, created)
    }

    private fun parseTransactions(arr: JSONArray): List<ParsedTx> = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        ParsedTx(
            date = LocalDate.parse(o.getString("date")),
            // Missing/unknown "type" falls back to the pre-Savings sign rule, so an older export always round-trips.
            type = o.optString("type", "").let { t -> runCatching { FlowType.valueOf(t) }.getOrNull() }
                ?: if (o.getDouble("amount") >= 0) FlowType.INCOME else FlowType.EXPENSE,
            category = o.optString("category", CsvImportParser.UNCATEGORIZED).ifBlank { CsvImportParser.UNCATEGORIZED },
            description = o.optString("description", ""),
            amount = o.getDouble("amount"),
            fundingSource = o.optString("fundingSource", "").takeIf { it.isNotBlank() }?.let { FundingSource.valueOf(it) },
            contributionKind = o.optString("contributionKind", "").takeIf { it.isNotBlank() }?.let { ContributionKind.valueOf(it) },
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

    private fun parseForecasts(arr: JSONArray): List<ForecastEntity> = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        ForecastEntity(
            note = o.getString("note"),
            amount = o.getDouble("amount"),
            date = o.optString("date", "").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
        )
    }
}
