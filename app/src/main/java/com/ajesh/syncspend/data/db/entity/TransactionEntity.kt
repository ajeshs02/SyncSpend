package com.ajesh.syncspend.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ajesh.syncspend.domain.model.ContributionKind
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.FundingSource
import java.time.LocalDate

/**
 * [amount] is signed (positive = income, negative = expense); [type] is a real stored column (not
 * re-derived from the sign) because `MIGRATION_5_6` once needed a Savings flow sign alone couldn't
 * distinguish from Income — Savings is gone now (see [com.ajesh.syncspend.data.db.entity.TransferEntity]),
 * but the stored column stays rather than reverting to a derived property, since existing rows already
 * carry it.
 *
 * [fundingSource]/[contributionKind] are **legacy and always null going forward** — leftovers from the
 * Savings-as-a-third-[FlowType] design, kept only so SQLite's `ALTER TABLE` history doesn't need a
 * destructive table rebuild to drop them. Nothing reads or writes them anymore.
 *
 * [description] is the entry's optional note: blank when there is none. It is never auto-filled
 * with the category name, since the row already shows the (live) category.
 *
 * [timeMinuteOfDay] is when the entry happened, 0..1439, or null when unknown. It is only known
 * for entries logged on the day they are dated; back-dated and imported entries have none.
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
    val timeMinuteOfDay: Int? = null,
    val type: FlowType = if (amount >= 0) FlowType.INCOME else FlowType.EXPENSE,
    val fundingSource: FundingSource? = null,
    val contributionKind: ContributionKind? = null,
)
