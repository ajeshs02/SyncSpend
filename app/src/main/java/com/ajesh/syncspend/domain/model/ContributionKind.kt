package com.ajesh.syncspend.domain.model

/**
 * Only meaningful for [FlowType.SAVINGS] rows (via
 * [com.ajesh.syncspend.data.db.entity.TransactionEntity.contributionKind]):
 * - [NEW_INCOME]: money just received and allocated straight to savings. It counts toward "Total
 *   Income" (it's genuinely new money) *and* increases the savings balance.
 * - [TRANSFER]: moving already-recorded income into savings. It must NOT count toward Income again
 *   (it was already counted when first recorded) — it only moves money from the regular-funds pool
 *   to the savings pool. See [com.ajesh.syncspend.domain.analytics.AnalyticsEngine] for the exact
 *   accounting formulas this distinction feeds into.
 */
enum class ContributionKind { NEW_INCOME, TRANSFER }
