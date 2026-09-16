package com.ajesh.syncspend.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ajesh.syncspend.domain.model.BillingCycle
import java.time.LocalDate

/**
 * Notify-only (confirmed product decision): a due subscription posts a real
 * AlarmManager notification but never auto-inserts a [TransactionEntity].
 * [categoryId] is nullable and used only to prefill Add Entry from the
 * notification's deep link.
 */
@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    val amount: Double,
    val billingCycle: BillingCycle,
    val nextDueDate: LocalDate,
    val categoryId: Long?,
    val active: Boolean,
)
