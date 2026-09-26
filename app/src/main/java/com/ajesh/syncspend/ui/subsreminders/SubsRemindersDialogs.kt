package com.ajesh.syncspend.ui.subsreminders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.ReminderSchedule
import com.ajesh.syncspend.ui.components.ConfirmDialog
import com.ajesh.syncspend.ui.components.DatePickerSheet
import com.ajesh.syncspend.ui.components.DesignSheet
import com.ajesh.syncspend.ui.components.DesignTextField
import com.ajesh.syncspend.ui.components.IconPickTile
import com.ajesh.syncspend.ui.components.IconPickerDialog
import com.ajesh.syncspend.ui.components.NameIconDialog
import com.ajesh.syncspend.ui.components.PickerChip
import com.ajesh.syncspend.ui.components.PrimaryButton
import com.ajesh.syncspend.ui.components.SheetDeleteButton
import com.ajesh.syncspend.ui.components.SheetHeader
import com.ajesh.syncspend.ui.components.TimePickerSheet
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

// ---------------------------------------------------------------------------
// Shared pieces: the same fields appear in the centered "New …" dialogs and in
// the Edit Entry-style bottom sheets that open when a row is tapped.
// ---------------------------------------------------------------------------

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = SyncSpendTheme.colors.sub,
        modifier = Modifier.padding(top = 14.dp, bottom = 7.dp),
    )
}

/** A tile that shows a value and opens a picker when tapped. */
@Composable
private fun PickerTile(icon: ImageVector, text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = SyncSpendTheme.colors
    Row(
        modifier = modifier
            .background(colors.tile, RoundedCornerShape(14.dp))
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = colors.ink, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
    }
}

private val amountPattern = Regex("""\d*\.?\d{0,2}""")

private fun formatAmount(value: Double?): String =
    value?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""

@Composable
private fun SubscriptionFields(
    currencySymbol: String,
    amountText: String,
    onAmountChange: (String) -> Unit,
    cycle: BillingCycle,
    onCycleChange: (BillingCycle) -> Unit,
    due: LocalDate,
    onPickDue: () -> Unit,
) {
    FieldLabel("Amount")
    DesignTextField(
        value = amountText,
        onValueChange = { v -> if (v.isEmpty() || v.matches(amountPattern)) onAmountChange(v) },
        placeholder = "${currencySymbol}0.00",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
    FieldLabel("Billing cycle")
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        BillingCycle.entries.forEach { c ->
            PickerChip(
                label = c.name.lowercase().replaceFirstChar { it.uppercase() },
                selected = cycle == c,
                modifier = Modifier.weight(1f),
            ) { onCycleChange(c) }
        }
    }
    FieldLabel("Next due date")
    PickerTile(SyncSpendIcons.Cal, DateUtils.shortDateYear(due), Modifier.fillMaxWidth(), onPickDue)
}

@Composable
private fun ReminderFields(
    schedule: ReminderSchedule,
    onScheduleChange: (ReminderSchedule) -> Unit,
    date: LocalDate,
    onPickDate: () -> Unit,
    minute: Int,
    onPickTime: () -> Unit,
) {
    FieldLabel("Repeat")
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        ReminderSchedule.entries.forEach { s ->
            PickerChip(
                label = s.name.lowercase().replaceFirstChar { it.uppercase() },
                selected = schedule == s,
                modifier = Modifier.weight(1f),
            ) { onScheduleChange(s) }
        }
    }
    FieldLabel(if (schedule == ReminderSchedule.ONCE) "Date" else "Starting")
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
        PickerTile(SyncSpendIcons.Cal, DateUtils.shortDate(date), Modifier.weight(1f), onPickDate)
        PickerTile(SyncSpendIcons.Clock, DateUtils.fmt12(minute), Modifier.weight(1f), onPickTime)
    }
}

/** Future dates only: from this month to a year ahead (enough for yearly renewals). */
@Composable
private fun FutureDatePicker(initial: LocalDate, title: String, onApply: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    DatePickerSheet(
        initial = initial,
        title = title,
        minMonth = YearMonth.now(),
        maxMonth = YearMonth.now().plusMonths(12),
        onApply = onApply,
        onDismiss = onDismiss,
    )
}

// ---------------------------------------------------------------------------
// Add: the design's centered modal dialogs
// ---------------------------------------------------------------------------

@Composable
fun SubscriptionDialog(
    currencySymbol: String,
    onSave: (name: String, iconKey: String, amount: Double, cycle: BillingCycle, due: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf(BillingCycle.MONTHLY) }
    var due by remember { mutableStateOf(LocalDate.now().plusMonths(1)) }
    var pickingDue by remember { mutableStateOf(false) }
    val amount = amountText.toDoubleOrNull()

    NameIconDialog(
        title = "New subscription",
        body = "Add the service, amount and billing cycle. You'll get a notification on the due date.",
        cta = "Add",
        initialName = "",
        initialIconKey = "card",
        namePlaceholder = "Netflix, Spotify, Gym…",
        extraValid = amount != null && amount > 0,
        onConfirm = { name, icon -> onSave(name, icon, amount ?: 0.0, cycle, due) },
        onDismiss = onDismiss,
    ) {
        SubscriptionFields(currencySymbol, amountText, { amountText = it }, cycle, { cycle = it }, due) { pickingDue = true }
    }

    if (pickingDue) FutureDatePicker(due, "NEXT DUE DATE", { due = it }) { pickingDue = false }
}

@Composable
fun ReminderDialog(
    onSave: (label: String, iconKey: String, schedule: ReminderSchedule, date: LocalDate, minuteOfDay: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var schedule by remember { mutableStateOf(ReminderSchedule.DAILY) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var minute by remember { mutableStateOf(20 * 60) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }

    NameIconDialog(
        title = "New reminder",
        body = "Choose what to be reminded about and when. It arrives as a real notification.",
        cta = "Add",
        initialName = "",
        initialIconKey = "clock",
        namePlaceholder = "Log today's spend, Rent due…",
        onConfirm = { name, icon -> onSave(name, icon, schedule, date, minute) },
        onDismiss = onDismiss,
    ) {
        ReminderFields(schedule, { schedule = it }, date, { pickingDate = true }, minute) { pickingTime = true }
    }

    if (pickingDate) {
        FutureDatePicker(date, if (schedule == ReminderSchedule.ONCE) "REMINDER DATE" else "STARTING FROM", { date = it }) { pickingDate = false }
    }
    if (pickingTime) TimePickerSheet(minute, onApply = { minute = it }, onDismiss = { pickingTime = false })
}

// ---------------------------------------------------------------------------
// Edit: bottom sheets styled like Edit Entry (tap a row to open)
// ---------------------------------------------------------------------------

@Composable
fun SubscriptionEditSheet(
    initial: SubscriptionEntity,
    currencySymbol: String,
    onSave: (name: String, iconKey: String, amount: Double, cycle: BillingCycle, due: LocalDate) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var iconKey by remember(initial.id) { mutableStateOf(initial.iconKey) }
    var amountText by remember(initial.id) { mutableStateOf(formatAmount(initial.amount)) }
    var cycle by remember(initial.id) { mutableStateOf(initial.billingCycle) }
    var due by remember(initial.id) { mutableStateOf(initial.nextDueDate) }
    var pickingIcon by remember { mutableStateOf(false) }
    var pickingDue by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val amount = amountText.toDoubleOrNull()
    val canSave = name.isNotBlank() && amount != null && amount > 0

    DesignSheet(onDismiss = onDismiss) { close ->
        SheetHeader("Edit subscription", onClose = close)
        Row(
            modifier = Modifier.padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconPickTile(iconKey, onClick = { pickingIcon = true })
            DesignTextField(value = name, onValueChange = { name = it }, placeholder = "Name", modifier = Modifier.weight(1f))
        }
        SubscriptionFields(currencySymbol, amountText, { amountText = it }, cycle, { cycle = it }, due) { pickingDue = true }

        Row(modifier = Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SheetDeleteButton(onClick = { confirmDelete = true })
            PrimaryButton(
                text = "Save Changes",
                enabled = canSave,
                height = 48.dp,
                radius = 16.dp,
                modifier = Modifier.weight(1f),
                onClick = {
                    onSave(name.trim(), iconKey, amount ?: return@PrimaryButton, cycle, due)
                    close()
                },
            )
        }

        if (pickingIcon) IconPickerDialog(iconKey, onPick = { iconKey = it; pickingIcon = false }, onDismiss = { pickingIcon = false })
        if (pickingDue) FutureDatePicker(due, "NEXT DUE DATE", { due = it }) { pickingDue = false }
        if (confirmDelete) {
            ConfirmDialog(
                title = "Delete subscription?",
                body = "“${initial.name}” will stop appearing on your list and its reminder is cancelled.",
                cta = "Delete",
                onConfirm = {
                    confirmDelete = false
                    onDelete()
                    close()
                },
                onDismiss = { confirmDelete = false },
            )
        }
    }
}

@Composable
fun ReminderEditSheet(
    initial: ReminderEntity,
    onSave: (label: String, iconKey: String, schedule: ReminderSchedule, date: LocalDate, minuteOfDay: Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember(initial.id) { mutableStateOf(initial.label) }
    var iconKey by remember(initial.id) { mutableStateOf(initial.iconKey) }
    var schedule by remember(initial.id) { mutableStateOf(initial.schedule) }
    var date by remember(initial.id) { mutableStateOf(initial.nextTriggerDate) }
    var minute by remember(initial.id) { mutableStateOf(initial.timeMinuteOfDay) }
    var pickingIcon by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    DesignSheet(onDismiss = onDismiss) { close ->
        SheetHeader("Edit reminder", onClose = close)
        Row(
            modifier = Modifier.padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconPickTile(iconKey, onClick = { pickingIcon = true })
            DesignTextField(value = label, onValueChange = { label = it }, placeholder = "Reminder", modifier = Modifier.weight(1f))
        }
        ReminderFields(schedule, { schedule = it }, date, { pickingDate = true }, minute) { pickingTime = true }

        Row(modifier = Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SheetDeleteButton(onClick = { confirmDelete = true })
            PrimaryButton(
                text = "Save Changes",
                enabled = label.isNotBlank(),
                height = 48.dp,
                radius = 16.dp,
                modifier = Modifier.weight(1f),
                onClick = {
                    onSave(label.trim(), iconKey, schedule, date, minute)
                    close()
                },
            )
        }

        if (pickingIcon) IconPickerDialog(iconKey, onPick = { iconKey = it; pickingIcon = false }, onDismiss = { pickingIcon = false })
        if (pickingDate) {
            FutureDatePicker(date, if (schedule == ReminderSchedule.ONCE) "REMINDER DATE" else "STARTING FROM", { date = it }) { pickingDate = false }
        }
        if (pickingTime) TimePickerSheet(minute, onApply = { minute = it }, onDismiss = { pickingTime = false })
        if (confirmDelete) {
            ConfirmDialog(
                title = "Delete reminder?",
                body = "“${initial.label}” will stop appearing on your list and its notification is cancelled.",
                cta = "Delete",
                onConfirm = {
                    confirmDelete = false
                    onDelete()
                    close()
                },
                onDismiss = { confirmDelete = false },
            )
        }
    }
}
