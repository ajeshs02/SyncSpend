package com.ajesh.syncspend.ui.subsreminders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.ReminderSchedule
import com.ajesh.syncspend.ui.components.DesignTextField
import com.ajesh.syncspend.ui.components.NameIconDialog
import com.ajesh.syncspend.ui.components.PickerChip
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import com.ajesh.syncspend.util.NativePickers
import java.time.LocalDate

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

@Composable
fun SubscriptionDialog(
    initial: SubscriptionEntity?,
    currencySymbol: String,
    onSave: (name: String, iconKey: String, amount: Double, cycle: BillingCycle, due: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val dark = SyncSpendTheme.colors.isDark
    var amountText by remember { mutableStateOf(initial?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var cycle by remember { mutableStateOf(initial?.billingCycle ?: BillingCycle.MONTHLY) }
    var due by remember { mutableStateOf(initial?.nextDueDate ?: LocalDate.now().plusMonths(1)) }
    val amount = amountText.toDoubleOrNull()

    NameIconDialog(
        title = if (initial == null) "New subscription" else "Edit subscription",
        body = "Add the service, amount and billing cycle. You'll get a notification on the due date.",
        cta = if (initial == null) "Add" else "Save",
        initialName = initial?.name ?: "",
        initialIconKey = initial?.iconKey ?: "card",
        namePlaceholder = "Netflix, Spotify, Gym…",
        extraValid = amount != null && amount > 0,
        onConfirm = { name, icon -> onSave(name, icon, amount ?: 0.0, cycle, due) },
        onDismiss = onDismiss,
    ) {
        FieldLabel("Amount")
        DesignTextField(
            value = amountText,
            onValueChange = { v -> if (v.isEmpty() || v.matches(Regex("""\d*\.?\d{0,2}"""))) amountText = v },
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
                ) { cycle = c }
            }
        }
        FieldLabel("Next due date")
        PickerTile(SyncSpendIcons.Cal, "${DateUtils.shortDate(due)} ${due.year}", Modifier.fillMaxWidth()) {
            NativePickers.showDate(context, due, dark) { due = it }
        }
    }
}

@Composable
fun ReminderDialog(
    initial: ReminderEntity?,
    onSave: (label: String, iconKey: String, schedule: ReminderSchedule, date: LocalDate, minuteOfDay: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val dark = SyncSpendTheme.colors.isDark
    var schedule by remember { mutableStateOf(initial?.schedule ?: ReminderSchedule.DAILY) }
    var date by remember { mutableStateOf(initial?.nextTriggerDate ?: LocalDate.now()) }
    var minute by remember { mutableStateOf(initial?.timeMinuteOfDay ?: (20 * 60)) }

    NameIconDialog(
        title = if (initial == null) "New reminder" else "Edit reminder",
        body = "Choose what to be reminded about and when. It arrives as a real notification.",
        cta = if (initial == null) "Add" else "Save",
        initialName = initial?.label ?: "",
        initialIconKey = initial?.iconKey ?: "clock",
        namePlaceholder = "Log today's spend, Rent due…",
        onConfirm = { name, icon -> onSave(name, icon, schedule, date, minute) },
        onDismiss = onDismiss,
    ) {
        FieldLabel("Repeat")
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ReminderSchedule.entries.forEach { s ->
                PickerChip(
                    label = s.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = schedule == s,
                    modifier = Modifier.weight(1f),
                ) { schedule = s }
            }
        }
        FieldLabel(if (schedule == ReminderSchedule.ONCE) "Date" else "Starting")
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            PickerTile(SyncSpendIcons.Cal, DateUtils.shortDate(date), Modifier.weight(1f)) {
                NativePickers.showDate(context, date, dark) { date = it }
            }
            PickerTile(SyncSpendIcons.Clock, DateUtils.fmt12(minute), Modifier.weight(1f)) {
                NativePickers.showTime(context, minute, dark) { minute = it }
            }
        }
    }
}
