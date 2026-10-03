package com.ajesh.syncspend.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * A planned/forecast expense the user jotted down for a month. [date] is the 1st of the chosen month
 * (current or next — see `ForecastDialog`'s month chips); `null` only ever appears on rows created
 * before that chip picker existed, meaning "this month," read fresh against the current date.
 *
 * [categoryId] is an Expense category (nullable: forecasts could be created before this field
 * existed, or left uncategorized). [completed] is an explicit, persistent "done" flag — independent
 * of whether an actual Expense was ever created for it. [completedTransactionId] is set only once
 * "Mark Done & Add Expense" actually inserts that [TransactionEntity]; it is the duplicate-prevention
 * gate (not [completed] alone — see `ForecastViewModel`), and is cleared back to null if that linked
 * transaction is later deleted, without reverting [completed] (see `TransactionRepository.delete`).
 */
@Entity(tableName = "forecasts")
data class ForecastEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val note: String,
    val amount: Double,
    val date: LocalDate? = null,
    val categoryId: Long? = null,
    val completed: Boolean = false,
    val completedTransactionId: Long? = null,
)
