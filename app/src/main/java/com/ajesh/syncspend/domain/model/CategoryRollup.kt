package com.ajesh.syncspend.domain.model

data class CategoryRollup(
    val categoryId: Long,
    val name: String,
    val iconKey: String,
    val count: Int,
    val totalAbs: Double,
    val sharePercent: Int,
)
