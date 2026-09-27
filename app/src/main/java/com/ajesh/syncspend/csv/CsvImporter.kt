package com.ajesh.syncspend.csv

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.ajesh.syncspend.data.db.AppDatabase
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.model.FlowType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportResult(
    val imported: Int,
    /** Rows that couldn't be read at all. */
    val skipped: Int,
    /** Rows already in the app (identical date, amount, category and description). */
    val duplicates: Int,
    val problems: List<String>,
    val createdCategories: Int,
)

object CsvImporter {
    /**
     * Reads a CSV written by this app *or* the previous tracker (see
     * [CsvImportParser] for the accepted columns). Categories are matched by
     * (name, type) case-insensitively; a match that was archived is revived and
     * a missing one is created with a guessed icon. Rows already present are
     * skipped, so re-importing a file is harmless. All writes happen in one
     * transaction.
     */
    suspend fun import(context: Context, uri: Uri, db: AppDatabase): ImportResult = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not open the file")
        val parsed = CsvImportParser.parse(CsvImportParser.decode(bytes))
        parsed.headerError?.let { return@withContext ImportResult(0, 0, 0, listOf(it), 0) }

        var created = 0
        var duplicates = 0
        val toInsert = ArrayList<TransactionEntity>(parsed.rows.size)

        db.withTransaction {
            val categoryDao = db.categoryDao()
            val existing = DuplicateTracker(db.transactionDao().getAllOnce())

            val byKey = HashMap<Pair<FlowType, String>, CategoryEntity>()
            val nextOrder = HashMap<FlowType, Int>()
            categoryDao.getAllOnce().forEach { c ->
                byKey.putIfAbsent(c.type to c.name.trim().lowercase(), c)
                nextOrder[c.type] = maxOf(nextOrder[c.type] ?: 0, c.sortOrder + 1)
            }

            val now = System.currentTimeMillis()
            parsed.rows.forEachIndexed { index, row ->
                val key = row.type to row.categoryName.trim().lowercase()
                var category = byKey[key]
                if (category == null) {
                    val fresh = CategoryEntity(
                        name = row.categoryName.trim(),
                        iconKey = CategoryIconGuesser.guess(row.categoryName),
                        type = row.type,
                        sortOrder = nextOrder[row.type] ?: 0,
                    )
                    nextOrder[row.type] = fresh.sortOrder + 1
                    category = fresh.copy(id = categoryDao.insert(fresh))
                    byKey[key] = category
                    created++
                } else if (category.archived) {
                    category = category.copy(archived = false)
                    categoryDao.update(category)
                    byKey[key] = category
                }

                val signed = if (row.type == FlowType.INCOME) row.amount else -row.amount
                // The description is the entry's optional note: blank stays blank (the row shows its category).
                val description = row.description
                if (existing.consumeIfDuplicate(row.date, signed, category.id, description)) {
                    duplicates++
                } else {
                    toInsert += TransactionEntity(
                        amount = signed,
                        description = description,
                        categoryId = category.id,
                        date = row.date,
                        createdAt = now + index,
                    )
                }
            }
            db.transactionDao().insertAll(toInsert)
        }
        ImportResult(toInsert.size, parsed.skipped, duplicates, parsed.problems, created)
    }
}
