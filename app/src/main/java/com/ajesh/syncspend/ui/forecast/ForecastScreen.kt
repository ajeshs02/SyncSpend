package com.ajesh.syncspend.ui.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.data.db.entity.ForecastEntity
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.ui.components.ConfirmDialog
import com.ajesh.syncspend.ui.components.DatePickerSheet
import com.ajesh.syncspend.ui.components.DesignDialog
import com.ajesh.syncspend.ui.components.DesignTextField
import com.ajesh.syncspend.ui.components.DialogButtons
import com.ajesh.syncspend.ui.components.DialogHeader
import com.ajesh.syncspend.ui.components.HeaderAddButton
import com.ajesh.syncspend.ui.components.PickerChip
import com.ajesh.syncspend.ui.components.SquareIconButton
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate

@Composable
fun ForecastScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ForecastViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ForecastViewModel(container.forecastRepository, container.preferencesRepository) }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SyncSpendTheme.colors

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ForecastEntity?>(null) }
    var deleting by remember { mutableStateOf<ForecastEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(top = SyncSpendChrome.screenTopInset)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SquareIconButton(
                SyncSpendIcons.Back, onBack, size = 36.dp, radius = 13.dp, iconSize = 17.dp,
                background = colors.pill,
                modifier = Modifier.border(1.dp, colors.line, RoundedCornerShape(13.dp)),
            )
            Text(
                "Forecast",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = colors.ink,
                modifier = Modifier.weight(1f).padding(vertical = 3.dp),
            )
            HeaderAddButton("Add", onClick = { showAdd = true })
        }

        Column(
            modifier = Modifier
                .padding(start = 22.dp, end = 22.dp, top = 16.dp)
                .fillMaxWidth()
                .background(colors.mintGradient, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Text("Forecast this month", fontSize = 11.sp, color = colors.mink.copy(alpha = 0.7f))
            Text(
                state.summaryFormatted,
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                letterSpacing = (-0.24).sp,
                color = colors.mink,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (state.rows.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 40.dp, end = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nothing forecast yet", style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                Text(
                    "Tap Add to jot down a planned expense for the month.",
                    style = MaterialTheme.typography.bodySmall, color = colors.sub,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 12.dp, bottom = SyncSpendChrome.screenBottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(state.rows, key = { it.entity.id }, contentType = { "row" }) { row ->
                    ForecastRow(row = row, onClick = { editing = row.entity }, modifier = Modifier.animateItem())
                }
            }
        }
    }

    if (showAdd) {
        ForecastDialog(
            initial = null,
            currencySymbol = state.currencySymbol,
            onSave = { note, amount, date ->
                viewModel.save(null, note, amount, date)
                showAdd = false
            },
            onDelete = null,
            onDismiss = { showAdd = false },
        )
    }
    editing?.let { forecast ->
        ForecastDialog(
            initial = forecast,
            currencySymbol = state.currencySymbol,
            onSave = { note, amount, date ->
                viewModel.save(forecast, note, amount, date)
                editing = null
            },
            onDelete = { editing = null; deleting = forecast },
            onDismiss = { editing = null },
        )
    }
    deleting?.let { forecast ->
        ConfirmDialog(
            title = "Delete this forecast?",
            body = "“${forecast.note}” will be removed.",
            cta = "Delete",
            onConfirm = { viewModel.delete(forecast); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun ForecastRow(row: ForecastRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SyncSpendTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card, RoundedCornerShape(16.dp))
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(colors.tile, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(SyncSpendIcons.Cal, null, tint = colors.ink, modifier = Modifier.size(16.dp)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.note,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink,
                maxLines = 1,
                textDecoration = if (row.struckThrough) TextDecoration.LineThrough else null,
            )
            Text(row.dateLabel, fontSize = 10.5.sp, color = colors.sub, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            row.amountFormatted,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
            textDecoration = if (row.struckThrough) TextDecoration.LineThrough else null,
        )
    }
}

@Composable
private fun ForecastDialog(
    initial: ForecastEntity?,
    currencySymbol: String,
    onSave: (note: String, amount: Double, date: LocalDate?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val colors = SyncSpendTheme.colors
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var amountText by remember { mutableStateOf(initial?.let { trimAmount(it.amount) } ?: "") }
    var date by remember { mutableStateOf(initial?.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }
    val parsedAmount = amountText.toDoubleOrNull()
    val valid = note.isNotBlank() && parsedAmount != null && parsedAmount > 0

    DesignDialog(onDismiss = onDismiss) {
        DialogHeader(
            icon = SyncSpendIcons.Cal,
            iconTint = colors.ink,
            title = if (initial == null) "New forecast" else "Edit forecast",
            body = "Jot down a planned expense for the month.",
        )
        FieldLabel("Note", top = 14.dp)
        DesignTextField(
            value = note,
            onValueChange = { note = it },
            placeholder = "e.g. Rent",
            error = attempted && note.isBlank(),
        )
        FieldLabel("Amount", top = 12.dp)
        DesignTextField(
            value = amountText,
            onValueChange = { v -> if (v.matches(Regex("""\d*\.?\d{0,2}"""))) amountText = v },
            placeholder = currencySymbol + "0",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            error = attempted && (parsedAmount == null || parsedAmount <= 0),
        )
        FieldLabel("When", top = 12.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PickerChip(label = "This month", selected = date == null, modifier = Modifier.weight(1f)) { date = null }
            PickerChip(
                label = date?.let { DateUtils.shortDate(it) } ?: "Pick a date",
                selected = date != null,
                modifier = Modifier.weight(1f),
            ) { showDatePicker = true }
        }
        DialogButtons(
            cta = if (initial == null) "Add" else "Save",
            onCancel = onDismiss,
            onConfirm = { if (valid) onSave(note.trim(), parsedAmount!!, date) else attempted = true },
        )
        if (onDelete != null) {
            Text(
                "Delete forecast",
                style = MaterialTheme.typography.labelLarge,
                color = colors.neg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDelete),
            )
        }
    }
    if (showDatePicker) {
        DatePickerSheet(
            initial = date ?: LocalDate.now(),
            onApply = { date = it; showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun FieldLabel(text: String, top: androidx.compose.ui.unit.Dp) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = SyncSpendTheme.colors.sub,
        modifier = Modifier.padding(top = top, bottom = 5.dp),
    )
}

/** Preserves real cents instead of rounding them away, same rule as Edit Entry's own amount field. */
private fun trimAmount(v: Double): String {
    val totalCents = Math.round(v * 100)
    val whole = totalCents / 100
    val cents = totalCents % 100
    return if (cents == 0L) whole.toString() else "$whole.${cents.toString().padStart(2, '0')}"
}
