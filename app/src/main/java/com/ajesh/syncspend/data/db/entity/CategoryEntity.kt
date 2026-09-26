package com.ajesh.syncspend.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ajesh.syncspend.domain.model.FlowType

/** Icon-only, no color — per spec. [sortOrder] drives the up/down reorder on the Categories screen. */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    val type: FlowType,
    val sortOrder: Int,
    /**
     * Deleting a category that past entries still reference archives it instead
     * of removing the row, so those entries keep their real name and icon
     * (spec: "past entries keep their label"). Archived categories are hidden
     * from pickers and the Categories screen.
     */
    val archived: Boolean = false,
)
