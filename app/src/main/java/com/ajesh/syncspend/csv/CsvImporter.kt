package com.ajesh.syncspend.csv

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.ajesh.syncspend.data.db.AppDatabase
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.model.FlowType
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportResult(val imported: Int, val skipped: Int, val problems: List<String>, val createdCategories: Int)

object CsvImporter {
    /**
     * Reads the app's own export format. Categories are matched by
     * (name, type) case-insensitively and created (default icon) when missing;
     * an archived match is revived. All inserts happen in one transaction.
     * Re-importing the same file adds the rows again — no de-duplication.
     */
    suspend fun import(context: Context, uri: Uri, db: AppDatabase): ImportResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("Could not open the file")
        val rows = CsvFormat.parse(text.removePrefix("\uFEFF"))
        if (rows.isEmpty()) return@withContext ImportResult(0, 0, listOf("The file is empty."), 0)

        val header = rows.first().map { it.trim().lowercase() }
        if (header != CsvFormat.HEADER) {
            return@withContext ImportResult(0, rows.size - 1, listOf("Not a SyncSpend export — expected header: ${CsvFormat.HEADER.joinToString(",")}"), 0)
        }

        var imported = 0
        var skipped = 0
        var created = 0
        val problems = mutableListOf<String>()

        db.withTransaction {
            val categoryDao = db.categoryDao()
            val txDao = db.transactionDao()
            val known = categoryDao.getAllOnce().toMutableList()
            val nextOrder = FlowType.values().associateWith { t ->
                (known.filter { it.type == t }.maxOfOrNull { it.sortOrder } ?: -1) + 1
            }.toMutableMap()

            rows.drop(1).forEachIndexed { index, r ->
                val line = index + 2
                fun skip(reason: String) {
                    skipped++
                    if (problems.size < 5) problems += "Row $line: $reason"
                }
                if (r.size < 5) return@forEachIndexed skip("expected 5 columns")
                val date = try { LocalDate.parse(r[0].trim()) } catch (_: DateTimeParseException) { return@forEachIndexed skip("bad date \"${r[0]}\"") }
                val type = when (r[1].trim().uppercase()) {
                    "EXPENSE" -> FlowType.EXPENSE
                    "INCOME" -> FlowType.INCOME
                    else -> return@forEachIndexed skip("bad type \"${r[1]}\"")
                }
                val categoryName = r[2].trim()
                if (categoryName.isEmpty()) return@forEachIndexed skip("missing category")
                val amount = r[4].trim().toDoubleOrNull()?.takeIf { it > 0 } ?: return@forEachIndexed skip("bad amount \"${r[4]}\"")

                var category = known.find { it.type == type && it.name.equals(categoryName, ignoreCase = true) }
                if (category == null) {
                    val fresh = CategoryEntity(name = categoryName, iconKey = "receipt", type = type, sortOrder = nextOrder.getValue(type))
                    nextOrder[type] = nextOrder.getValue(type) + 1
                    category = fresh.copy(id = categoryDao.insert(fresh))
                    known += category
                    created++
                } else if (category.archived) {
                    val revived = category.copy(archived = false)
                    categoryDao.update(revived)
                    known[known.indexOf(category)] = revived
                    category = revived
                }
                txDao.insert(
                    TransactionEntity(
                        amount = if (type == FlowType.INCOME) amount else -amount,
                        description = r[3].trim().ifEmpty { categoryName },
                        categoryId = category.id,
                        date = date,
                        createdAt = System.currentTimeMillis() + index,
                    ),
                )
                imported++
            }
        }
        ImportResult(imported, skipped, problems, created)
    }
}
