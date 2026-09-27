package com.ajesh.syncspend.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.RemindOffsets
import java.time.LocalDate

/**
 * Notify-only (confirmed product decision): a due subscription posts a real
 * AlarmManager notification but never auto-inserts a [TransactionEntity].
 * [categoryId] is nullable and used only to prefill Add Entry from the
 * notification's deep link.
 *
 * [remindDaysBefore] is the "remind me N days before" set (see [RemindOffsets]); the due day itself
 * always notifies.
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
    @ColumnInfo(defaultValue = RemindOffsets.DEFAULT_TEXT) val remindDaysBefore: String = RemindOffsets.DEFAULT_TEXT,
    /** Minute-of-day used for the due-date alert and every "N days before" alert alike. */
    @ColumnInfo(defaultValue = "540") val remindMinuteOfDay: Int = 540,
)
