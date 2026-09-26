package com.ajesh.syncspend.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ajesh.syncspend.domain.model.ReminderSchedule
import java.time.LocalDate

/** Notify-only, same as [SubscriptionEntity] — never auto-inserts a transaction. */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val iconKey: String,
    val schedule: ReminderSchedule,
    val timeMinuteOfDay: Int,
    val nextTriggerDate: LocalDate,
    val active: Boolean,
)
