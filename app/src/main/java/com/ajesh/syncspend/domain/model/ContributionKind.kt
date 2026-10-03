package com.ajesh.syncspend.domain.model

/**
 * Legacy: part of the Savings-as-a-third-[FlowType] design this app no longer uses — every Savings
 * contribution is now unconditionally a transfer (see [com.ajesh.syncspend.data.db.entity.TransferEntity]),
 * never optionally "new income". Kept only so
 * [com.ajesh.syncspend.data.db.entity.TransactionEntity.contributionKind] — a dead, always-null-going-forward
 * column left in place to avoid a destructive table rebuild — still has a type to deserialize against.
 */
enum class ContributionKind { NEW_INCOME, TRANSFER }
