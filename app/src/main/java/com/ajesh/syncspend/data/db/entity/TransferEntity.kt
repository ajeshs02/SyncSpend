package com.ajesh.syncspend.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ajesh.syncspend.domain.model.TransferDirection
import java.time.LocalDate

/**
 * A Savings contribution ("Add to Savings") or withdrawal, kept in its own table entirely separate
 * from [TransactionEntity] so it structurally cannot inflate Income/Expense totals — there's nothing
 * to exclude in [com.ajesh.syncspend.domain.analytics.AnalyticsEngine] because a transfer never
 * appears in the `transactions` list those functions read.
 *
 * [amount] is always positive; [direction] says which way it moved. [categoryId] is a source/category
 * classification for a [TransferDirection.TO_SAVINGS] row — reusing the existing Income category list
 * (see `DefaultCategories`) rather than a parallel category system — and is always null for
 * [TransferDirection.FROM_SAVINGS] (a withdrawal has no "source"). Like [TransactionEntity.categoryId],
 * it has no foreign-key cascade: deleting the category archives it instead (see `CategoriesViewModel`),
 * so a transfer never loses its category label.
 */
@Entity(tableName = "transfers", indices = [Index("date")])
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val direction: TransferDirection,
    val date: LocalDate,
    val categoryId: Long? = null,
    val note: String = "",
    val createdAt: Long,
    val timeMinuteOfDay: Int? = null,
)
