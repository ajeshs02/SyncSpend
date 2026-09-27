package com.ajesh.syncspend.ui.subsreminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajesh.syncspend.alarm.AlarmScheduler
import com.ajesh.syncspend.alarm.AlarmTimes
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.data.repository.ReminderRepository
import com.ajesh.syncspend.data.repository.SubscriptionRepository
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.ReminderSchedule
import com.ajesh.syncspend.domain.model.RemindOffsets
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One list row (a subscription or a reminder). The daily log reminder lives in Settings only. */
data class ListRowUi(
    val id: Long,
    val name: String,
    val meta: String,
    val iconKey: String,
    val active: Boolean = true,
)

/** [id] (the row a notification was about) moved to the top, the rest in their order; unchanged when it isn't in the list. */
internal fun List<ListRowUi>.withHighlightFirst(id: Long?): List<ListRowUi> {
    val hit = if (id == null) -1 else indexOfFirst { it.id == id }
    if (hit <= 0) return this
    return listOf(this[hit]) + filterIndexed { index, _ -> index != hit }
}

data class SubsRemindersUiState(
    val currencySymbol: String = "₹",
    val subscriptionRows: List<ListRowUi> = emptyList(),
    val reminderRows: List<ListRowUi> = emptyList(),
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val monthlyRecurring: String = "",
    val activeReminders: String = "",
)

/** Both lists (subscriptions and reminders) live in one ViewModel; the screen shows whichever mode it was opened in. */
class SubsRemindersViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val reminderRepository: ReminderRepository,
    preferencesRepository: PreferencesRepository,
    private val alarmScheduler: AlarmScheduler,
) : ViewModel() {

    val uiState: StateFlow<SubsRemindersUiState> = combine(
        subscriptionRepository.getAll(),
        reminderRepository.getAll(),
        preferencesRepository.preferences,
    ) { subs, reminders, prefs ->
        val cur = prefs.currencyCode.symbol
        val monthly = subs.filter { it.active }.sumOf {
            when (it.billingCycle) {
                BillingCycle.WEEKLY -> it.amount * 52.0 / 12.0
                BillingCycle.MONTHLY -> it.amount
                BillingCycle.YEARLY -> it.amount / 12.0
            }
        }
        val activeReminders = reminders.count { it.active }
        SubsRemindersUiState(
            currencySymbol = cur,
            subscriptions = subs,
            reminders = reminders,
            monthlyRecurring = cur + CurrencyFormatter.amount(monthly),
            activeReminders = "$activeReminders scheduled",
            subscriptionRows = subs.map {
                ListRowUi(
                    id = it.id,
                    name = it.name,
                    meta = "$cur${CurrencyFormatter.amount(it.amount)} · ${it.billingCycle.name.lowercase()} · ${DateUtils.shortDate(it.nextDueDate)}" +
                        if (it.active) "" else " · Paused",
                    iconKey = it.iconKey,
                    active = it.active,
                )
            },
            reminderRows = reminders.map {
                ListRowUi(it.id, it.label, reminderMeta(it), it.iconKey, active = it.active)
            },
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubsRemindersUiState())

    /**
     * [remindDaysBefore] are the "N days before" heads-ups (the due day itself always notifies);
     * [remindMinuteOfDay] is used for the due-date alert and every one of those heads-ups alike.
     */
    fun saveSubscription(
        existing: SubscriptionEntity?, name: String, iconKey: String, amount: Double, cycle: BillingCycle, due: LocalDate,
        remindDaysBefore: Collection<Int>, remindMinuteOfDay: Int,
    ) {
        viewModelScope.launch {
            val entity = (existing ?: SubscriptionEntity(name = name, iconKey = iconKey, amount = amount, billingCycle = cycle, nextDueDate = due, categoryId = null, active = true))
                .copy(
                    name = name, iconKey = iconKey, amount = amount, billingCycle = cycle, nextDueDate = due,
                    remindDaysBefore = RemindOffsets.format(remindDaysBefore), remindMinuteOfDay = remindMinuteOfDay,
                )
            val saved = if (existing == null) entity.copy(id = subscriptionRepository.insert(entity)) else entity.also { subscriptionRepository.update(it) }
            alarmScheduler.scheduleSubscription(saved)
        }
    }

    fun setSubscriptionActive(subscription: SubscriptionEntity, active: Boolean) {
        viewModelScope.launch {
            // While paused nothing advanced the due date; when resuming, show the next real one.
            val due = if (active) AlarmTimes.nextSubscriptionDate(subscription.nextDueDate, subscription.billingCycle, subscription.remindMinuteOfDay) else subscription.nextDueDate
            val updated = subscription.copy(active = active, nextDueDate = due)
            subscriptionRepository.update(updated)
            alarmScheduler.scheduleSubscription(updated)
        }
    }

    fun deleteSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            alarmScheduler.cancelSubscription(subscription.id)
            subscriptionRepository.delete(subscription)
        }
    }

    /** Editing keeps the on/off state the user chose (a paused reminder stays paused). */
    fun saveReminder(existing: ReminderEntity?, label: String, iconKey: String, schedule: ReminderSchedule, date: LocalDate, minuteOfDay: Int) {
        viewModelScope.launch {
            val entity = (existing ?: ReminderEntity(label = label, iconKey = iconKey, schedule = schedule, timeMinuteOfDay = minuteOfDay, nextTriggerDate = date, active = true))
                .copy(label = label, iconKey = iconKey, schedule = schedule, timeMinuteOfDay = minuteOfDay, nextTriggerDate = date)
            val saved = if (existing == null) entity.copy(id = reminderRepository.insert(entity)) else entity.also { reminderRepository.update(it) }
            alarmScheduler.scheduleReminder(saved)
        }
    }

    /**
     * Returns false — and changes nothing — when switching on a one-time reminder
     * whose moment has already passed; the caller should send the user to edit
     * its date instead of silently doing nothing.
     */
    fun setReminderActive(reminder: ReminderEntity, active: Boolean): Boolean {
        val next = if (active) AlarmTimes.nextReminderDate(reminder.nextTriggerDate, reminder.schedule, reminder.timeMinuteOfDay) else reminder.nextTriggerDate
        if (next == null) return false
        viewModelScope.launch {
            val updated = reminder.copy(active = active, nextTriggerDate = next)
            reminderRepository.update(updated)
            alarmScheduler.scheduleReminder(updated)
        }
        return true
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            alarmScheduler.cancelReminder(reminder.id)
            reminderRepository.delete(reminder)
        }
    }

    private fun reminderMeta(r: ReminderEntity): String {
        val time = DateUtils.fmt12(r.timeMinuteOfDay)
        val d = r.nextTriggerDate
        val whenText = when (r.schedule) {
            ReminderSchedule.DAILY -> "Every day"
            ReminderSchedule.WEEKLY -> "Every ${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)}"
            ReminderSchedule.MONTHLY -> "${ordinal(d.dayOfMonth)} monthly"
            ReminderSchedule.ONCE -> "${DateUtils.shortDate(d)} ${d.year}" + if (!r.active) " (done)" else ""
        }
        return "$whenText · $time" + if (!r.active && r.schedule != ReminderSchedule.ONCE) " · Paused" else ""
    }

    private fun ordinal(n: Int): String {
        val suffix = if (n in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
        return "$n$suffix"
    }
}
