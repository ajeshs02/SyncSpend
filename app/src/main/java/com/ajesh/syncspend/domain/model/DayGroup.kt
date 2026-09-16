package com.ajesh.syncspend.domain.model

import com.ajesh.syncspend.data.db.entity.TransactionEntity

data class DayGroup(
    val label: String,
    val items: List<TransactionEntity>,
    val totalAbs: Double,
)
