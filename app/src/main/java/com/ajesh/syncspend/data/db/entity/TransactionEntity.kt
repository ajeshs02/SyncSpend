package com.ajesh.syncspend.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ajesh.syncspend.domain.model.ContributionKind
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.FundingSource
import java.time.LocalDate

/**
 * [amount] is signed (positive = income or a savings contribution, negative = expense) — but sign
 * alone can no longer tell income apart from a savings contribution (both positive), so [type] is a
 * real stored column, the single source of truth for a transaction's flow (back-filled for every
 * pre-Savings row by `MIGRATION_5_6`, from the same sign rule this used to derive on the fly).
 *
 * [fundingSource] only matters for [FlowType.EXPENSE] rows: null (the common case) means
 * [FundingSource.REGULAR]. [contributionKind] only matters for [FlowType.SAVINGS] rows: null means
 * [ContributionKind.NEW_INCOME] (new money) as opposed to [ContributionKind.TRANSFER] (reallocating
 * already-recorded income, never double-counted as income again). Both are plain nullable columns
 * rather than a nested object, matching Room's simplest shape for "small, optional, type-specific
 * metadata." See [com.ajesh.syncspend.domain.analytics.AnalyticsEngine] for the exact formulas these
 * two fields feed into.
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
