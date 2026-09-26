package com.ajesh.syncspend.csv

import android.content.Context
import android.net.Uri
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.data.db.entity.type
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CsvExporter {
    /** Writes [transactions] (oldest first) to [uri]; returns the number of rows written. */
    suspend fun export(
        context: Context,
        uri: Uri,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
    ): Int = withContext(Dispatchers.IO) {
        val names = categories.associateBy { it.id }
        val sorted = transactions.sortedWith(compareBy<TransactionEntity> { it.date }.thenBy { it.createdAt })
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { out ->
            out.write(CsvFormat.row(CsvFormat.HEADER)); out.write("\n")
            sorted.forEach { t ->
                out.write(
                    CsvFormat.row(
                        listOf(
                            t.date.toString(),
                            t.type.name,
                            names[t.categoryId]?.name ?: "Uncategorized",
                            t.description,
                            Math.round(abs(t.amount)).toString(),
                        ),
                    ),
                )
                out.write("\n")
            }
        } ?: error("Could not open the file for writing")
        sorted.size
    }
}
