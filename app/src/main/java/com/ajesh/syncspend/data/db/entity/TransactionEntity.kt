package com.ajesh.syncspend.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ajesh.syncspend.domain.model.FlowType
import java.time.LocalDate

/**
 * [amount] is signed (positive = income, negative = expense) to mirror the
 * design's reference logic 1:1 — [type] is derived, not stored, so there is
 * only one source of truth for a transaction's flow direction.
 *
 * [categoryId] intentionally has no foreign-key cascade: deleting a category
 * must not delete or orphan-cascade its past transactions (spec: "past
 * entries keep their label"). Label resolution falls back to a
 * "Deleted category" placeholder when the id no longer resolves.
 */
@Entity(
    tableName = "transactions",
    indices = [Index("date"), Index("categoryId")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val description: String,
    val categoryId: Long,
    val date: LocalDate,
    val createdAt: Long,
)

val TransactionEntity.type: FlowType
    get() = if (amount >= 0) FlowType.INCOME else FlowType.EXPENSE
