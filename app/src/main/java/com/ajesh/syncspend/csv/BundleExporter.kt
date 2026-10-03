package com.ajesh.syncspend.csv

import android.content.Context
import android.net.Uri
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.ForecastEntity
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.data.db.entity.TransferEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * The "export more than one data type" bundle: a single versioned JSON file, one top-level key per
 * [BundleSection] actually selected. [CsvExporter] (plain transactions-only CSV) stays the format for
 * the common single-type case — this is only used when [sections] is more than just TRANSACTIONS, or
 * includes a non-transaction type (see `SettingsScreen`'s export flow for that routing decision).
 * Takes the rows to write directly (like [CsvExporter] does) rather than querying the database
 * itself, so a caller can pre-filter transactions to a date range while leaving the other sections —
 * which have no comparable "range" concept — unscoped.
 */
object BundleExporter {
    const val SCHEMA_VERSION = 1

    suspend fun export(
        context: Context,
        uri: Uri,
        sections: Set<BundleSection>,
        transactions: List<TransactionEntity> = emptyList(),
        categories: List<CategoryEntity> = emptyList(),
        reminders: List<ReminderEntity> = emptyList(),
        subscriptions: List<SubscriptionEntity> = emptyList(),
        forecasts: List<ForecastEntity> = emptyList(),
        transfers: List<TransferEntity> = emptyList(),
    ) = withContext(Dispatchers.IO) {
        val root = JSONObject().put("schemaVersion", SCHEMA_VERSION)
        val categoriesById = categories.associateBy { it.id }

        if (BundleSection.TRANSACTIONS in sections) {
            root.put("transactions", transactionsJson(transactions, categoriesById))
        }
        if (BundleSection.REMINDERS in sections) {
            val arr = JSONArray()
            reminders.forEach { r ->
                arr.put(
                    JSONObject()
                        .put("label", r.label)
                        .put("iconKey", r.iconKey)
                        .put("schedule", r.schedule.name)
                        .put("timeMinuteOfDay", r.timeMinuteOfDay)
                        .put("nextTriggerDate", r.nextTriggerDate.toString())
                        .put("active", r.active),
                )
            }
            root.put("reminders", arr)
        }
        if (BundleSection.SUBSCRIPTIONS in sections) {
            val arr = JSONArray()
            subscriptions.forEach { s ->
                val cat = s.categoryId?.let { categoriesById[it] }
                arr.put(
                    JSONObject()
                        .put("name", s.name)
                        .put("iconKey", s.iconKey)
                        .put("amount", s.amount)
                        .put("billingCycle", s.billingCycle.name)
                        .put("nextDueDate", s.nextDueDate.toString())
                        .put("category", cat?.name)
                        .put("categoryType", cat?.type?.name)
                        .put("active", s.active)
                        .put("remindDaysBefore", s.remindDaysBefore)
                        .put("remindMinuteOfDay", s.remindMinuteOfDay),
                )
            }
            root.put("subscriptions", arr)
        }
        if (BundleSection.FORECASTS in sections) {
            val arr = JSONArray()
            forecasts.forEach { f ->
                arr.put(
                    JSONObject()
                        .put("note", f.note)
                        .put("amount", f.amount)
                        .put("date", f.date?.toString())
                        .put("category", f.categoryId?.let { categoriesById[it]?.name })
                        .put("completed", f.completed),
                )
            }
            root.put("forecasts", arr)
        }
        if (BundleSection.TRANSFERS in sections) {
            root.put("transfers", transfersJson(transfers, categoriesById))
        }

        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { out ->
            out.write(root.toString())
        } ?: error("Could not open the file for writing")
    }

    /** Serializes the raw stored amount (never rounded/formatted) so a round-trip never loses cents. */
    private fun transactionsJson(transactions: List<TransactionEntity>, categories: Map<Long, CategoryEntity>): JSONArray {
        val arr = JSONArray()
        transactions.sortedWith(compareBy<TransactionEntity> { it.date }.thenBy { it.createdAt }).forEach { t ->
            arr.put(
                JSONObject()
                    .put("date", t.date.toString())
                    .put("type", t.type.name)
                    .put("category", categories[t.categoryId]?.name ?: CsvImportParser.UNCATEGORIZED)
                    .put("description", t.description)
                    .put("amount", t.amount),
            )
        }
        return arr
    }

    /** [TransferEntity.categoryId] is serialized by name (like a transaction's), never a raw id — see this object's doc. */
    private fun transfersJson(transfers: List<TransferEntity>, categories: Map<Long, CategoryEntity>): JSONArray {
        val arr = JSONArray()
        transfers.sortedWith(compareBy<TransferEntity> { it.date }.thenBy { it.createdAt }).forEach { t ->
            arr.put(
                JSONObject()
                    .put("date", t.date.toString())
                    .put("direction", t.direction.name)
                    .put("category", t.categoryId?.let { categories[it]?.name })
                    .put("note", t.note)
                    .put("amount", t.amount),
            )
        }
        return arr
    }
}
