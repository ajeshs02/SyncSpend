package com.ajesh.syncspend.data.db

import androidx.room.TypeConverter
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ReminderSchedule
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromFlowType(value: String?): FlowType? = value?.let(FlowType::valueOf)

    @TypeConverter
    fun toFlowType(type: FlowType?): String? = type?.name

    @TypeConverter
    fun fromBillingCycle(value: String?): BillingCycle? = value?.let(BillingCycle::valueOf)

    @TypeConverter
    fun toBillingCycle(cycle: BillingCycle?): String? = cycle?.name

    @TypeConverter
    fun fromReminderSchedule(value: String?): ReminderSchedule? = value?.let(ReminderSchedule::valueOf)

    @TypeConverter
    fun toReminderSchedule(schedule: ReminderSchedule?): String? = schedule?.name
}
