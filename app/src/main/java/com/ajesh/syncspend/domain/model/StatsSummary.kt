package com.ajesh.syncspend.domain.model

import com.ajesh.syncspend.data.db.entity.TransactionEntity

/** Everything the Stats tab shows, computed once from the scope's transactions. */
data class StatsSummary(
    val flow: FlowType,
    val scope: ScopePeriod,
    val total: Double,
    val previousTotal: Double,
    val entryCount: Int,
    val averagePerEntry: Double,
    val activeDays: Int,
    val dailyAverage: Double,
    /** Signed % change vs the previous period; null when there's nothing to compare against. */
    val trendPercent: Int?,
    val otherFlowTotal: Double,
    val topCategory: CategoryRollup?,
    val biggestEntry: TransactionEntity?,
)
