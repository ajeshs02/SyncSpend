package com.ajesh.syncspend.domain.model

/** Whether a transaction/category represents money going out or coming in. Savings moves live in [com.ajesh.syncspend.data.db.entity.TransferEntity] instead — a separate ledger, not a third flow. */
enum class FlowType { EXPENSE, INCOME }
