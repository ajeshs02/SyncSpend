package com.ajesh.syncspend.domain.model

/**
 * Which pool of money an Expense transaction was paid from — only meaningful for [FlowType.EXPENSE]
 * rows; null (via [com.ajesh.syncspend.data.db.entity.TransactionEntity.fundingSource]) means
 * [REGULAR]. The expense keeps its real category and counts in Expense totals either way — this only
 * steers which balance (regular funds vs. savings funds) gets debited. See
 * [com.ajesh.syncspend.domain.analytics.AnalyticsEngine] for the exact accounting formulas.
 */
enum class FundingSource { REGULAR, SAVINGS }
