package com.ajesh.syncspend.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * A planned/forecast expense the user jotted down for a month — a plain note-taking feature, not a
 * transaction: it never inserts into `transactions` and has no alarm/notification of its own (unlike
 * [ReminderEntity]). [date] null means "this month," read fresh against the current date rather than
 * materializing a specific day — so it keeps reading as "this month" for as long as the user leaves
 * it there, a deliberate simplification rather than a gap to engineer around.
 */
@Entity(tableName = "forecasts")
data class ForecastEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val note: String,
    val amount: Double,
    val date: LocalDate? = null,
)
