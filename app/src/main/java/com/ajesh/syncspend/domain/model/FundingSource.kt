package com.ajesh.syncspend.domain.model

/**
 * Legacy: part of the Savings-as-a-third-[FlowType] design this app no longer uses (see
 * [com.ajesh.syncspend.data.db.entity.TransferEntity] for the replacement). Kept only so
 * [com.ajesh.syncspend.data.db.entity.TransactionEntity.fundingSource] — a dead, always-null-going-forward
 * column left in place to avoid a destructive table rebuild — still has a type to deserialize against.
 * Nothing writes a non-null value here anymore.
 */
enum class FundingSource { REGULAR, SAVINGS }
