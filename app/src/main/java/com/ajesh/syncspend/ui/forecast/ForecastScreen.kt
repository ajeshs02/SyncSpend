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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.ForecastEntity
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.components.CategoryField
import com.ajesh.syncspend.ui.components.CategoryPickerSheet
import com.ajesh.syncspend.ui.components.ConfirmDialog
import com.ajesh.syncspend.ui.components.DatePickerSheet
import com.ajesh.syncspend.ui.components.DesignDialog
import com.ajesh.syncspend.ui.components.DesignTextField
import com.ajesh.syncspend.ui.components.DialogButtons
import com.ajesh.syncspend.ui.components.DialogHeader
import com.ajesh.syncspend.ui.components.HeaderAddButton
import com.ajesh.syncspend.ui.components.PickerChip
import com.ajesh.syncspend.ui.components.rememberDismissKeyboardThen
import com.ajesh.syncspend.ui.components.SquareIconButton
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ForecastScreen(onBack: () -> Unit, onAddCategory: (FlowType) -> Unit = {}) {
    val container = LocalAppContainer.current
    val viewModel: ForecastViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ForecastViewModel(
                    container.forecastRepository,
                    container.transactionRepository,
                    container.categoryRepository,
                    container.preferencesRepository,
                    container.forecastReviewDismissedThisSession,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SyncSpendTheme.colors

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ForecastEntity?>(null) }
    var deleting by remember { mutableStateOf<ForecastEntity?>(null) }
    var acting by remember { mutableStateOf<ForecastEntity?>(null) }
    var confirmingExpenseFor by remember { mutableStateOf<ForecastEntity?>(null) }

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
        Text(
            "Plan upcoming expenses and track what's completed",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = colors.sub,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 2.dp),
        )

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
            Text(
                "Next ${state.nextMonthFormatted} · ${state.completedCount} done · ${state.pendingCount} pending",
                fontSize = 11.sp,
                color = colors.mink.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp),
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
                    ForecastRow(row = row, onClick = { acting = row.entity }, modifier = Modifier.animateItem())
                }
            }
        }
    }

    if (showAdd) {
        ForecastDialog(
            initial = null,
            currencySymbol = state.currencySymbol,
            categories = state.expenseCategories,
            onSave = { note, amount, date, categoryId ->
                viewModel.save(null, note, amount, date, categoryId)
                showAdd = false
            },
            onDelete = null,
            onDismiss = { showAdd = false },
            onAddCategory = { onAddCategory(FlowType.EXPENSE) },
        )
    }
    editing?.let { forecast ->
        ForecastDialog(
            initial = forecast,
            currencySymbol = state.currencySymbol,
            categories = state.expenseCategories,
            onSave = { note, amount, date, categoryId ->
                viewModel.save(forecast, note, amount, date, categoryId)
                editing = null
            },
            onDelete = { editing = null; deleting = forecast },
            onDismiss = { editing = null },
            onAddCategory = { onAddCategory(FlowType.EXPENSE) },
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
    acting?.let { forecast ->
        ForecastActionSheet(
            forecast = forecast,
            onMarkDone = { viewModel.markDone(forecast); acting = null },
            onMarkUndone = { viewModel.markUndone(forecast); acting = null },
            onAddExpense = { acting = null; confirmingExpenseFor = forecast },
            onEdit = { acting = null; editing = forecast },
            onDelete = { acting = null; deleting = forecast },
            onDismiss = { acting = null },
        )
    }
    state.pendingReview?.let { pendingReview ->
        PastMonthReviewDialog(
            rows = pendingReview,
            onConfirm = { viewModel.reviewStaleForecasts(confirm = true) },
            onDismiss = { viewModel.reviewStaleForecasts(confirm = false) },
        )
    }
    confirmingExpenseFor?.let { forecast ->
        ForecastExpenseConfirmDialog(
            forecast = forecast,
            currencySymbol = state.currencySymbol,
            categories = state.expenseCategories,
            onConfirm = { amount, categoryId, date, note ->
                viewModel.addExpenseAndComplete(forecast, amount, categoryId, date, note)
                confirmingExpenseFor = null
            },
            onDismiss = { confirmingExpenseFor = null },
            onAddCategory = { onAddCategory(FlowType.EXPENSE) },
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
            Text(
                row.dateLabel + (row.categoryName?.let { " · $it" } ?: ""),
                fontSize = 10.5.sp,
                color = colors.sub,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            row.amountFormatted,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
            textDecoration = if (row.struckThrough) TextDecoration.LineThrough else null,
        )
    }
}

/**
 * Tapping a row opens this instead of jumping straight to Edit. Three states (see
 * [ForecastViewModel]'s doc): not completed shows both "mark done" actions; completed with no linked
 * expense still offers "Add Expense" (you can mark done now, link the real spend later); completed
 * with a linked expense is terminal — no more "mark done" actions at all, which is the actual
 * duplicate-prevention gate.
 */
@Composable
private fun ForecastActionSheet(
    forecast: ForecastEntity,
    onMarkDone: () -> Unit,
    onMarkUndone: () -> Unit,
    onAddExpense: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SyncSpendTheme.colors
    DesignDialog(onDismiss = onDismiss) {
        DialogHeader(
            icon = SyncSpendIcons.Cal,
            iconTint = colors.ink,
            title = forecast.note,
            body = if (forecast.completedTransactionId != null) "Already linked to a real expense." else "What would you like to do?",
        )
        Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (forecast.completedTransactionId == null) {
                if (!forecast.completed) {
                    ActionButton("Mark as Done", primary = true, onClick = onMarkDone)
                }
                ActionButton(if (forecast.completed) "Add Expense" else "Mark Done & Add Expense", primary = true, onClick = onAddExpense)
                if (forecast.completed) {
                    ActionButton("Mark as Undone", primary = false, onClick = onMarkUndone)
                }
            }
            ActionButton("Edit", primary = false, onClick = onEdit)
            ActionButton("Delete", primary = false, destructive = true, onClick = onDelete)
        }
    }
}

/** Shown once a calendar month has moved on and left pending (uncompleted) forecasts behind. Confirming
 * marks them completed without deleting them; dismissing only suppresses the popup for this app session. */
@Composable
private fun PastMonthReviewDialog(rows: List<ForecastRowUi>, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = SyncSpendTheme.colors
    DesignDialog(onDismiss = onDismiss) {
        DialogHeader(
            icon = SyncSpendIcons.Cal,
            iconTint = colors.ink,
            title = "Last month's forecasts",
            body = "These are still marked pending from a month that's already ended. Mark them completed to clean up; nothing is deleted.",
        )
        Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${row.note} · ${row.dateLabel}", style = MaterialTheme.typography.bodySmall, color = colors.ink, modifier = Modifier.weight(1f))
                    Text(row.amountFormatted, style = MaterialTheme.typography.bodySmall, color = colors.sub)
                }
            }
        }
        Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            ActionButton("Mark completed", primary = true, onClick = onConfirm)
            ActionButton("Not now", primary = false, onClick = onDismiss)
        }
    }
}

@Composable
private fun ActionButton(label: String, primary: Boolean, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = SyncSpendTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(if (primary) colors.button else colors.card, RoundedCornerShape(15.dp))
            .border(1.dp, if (primary) Color.Transparent else colors.line, RoundedCornerShape(15.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                primary -> colors.onButton
                destructive -> colors.neg
                else -> colors.ink
            },
        )
    }
}

@Composable
private fun ForecastDialog(
    initial: ForecastEntity?,
    currencySymbol: String,
    categories: List<CategoryEntity>,
    onSave: (note: String, amount: Double, date: LocalDate, categoryId: Long?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    onAddCategory: () -> Unit = {},
) {
    val colors = SyncSpendTheme.colors
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var amountText by remember { mutableStateOf(initial?.let { trimAmount(it.amount) } ?: "") }
    val currentMonth = remember { YearMonth.now() }
    val nextMonth = remember { currentMonth.plusMonths(1) }
    var month by remember { mutableStateOf(initial?.date?.let { YearMonth.from(it) } ?: currentMonth) }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }
    val dismissKeyboardThen = rememberDismissKeyboardThen()
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
            PickerChip(label = DateUtils.monthYearLabel(currentMonth.atDay(1)), selected = month == currentMonth, modifier = Modifier.weight(1f)) {
                month = currentMonth
            }
            PickerChip(label = DateUtils.monthYearLabel(nextMonth.atDay(1)), selected = month == nextMonth, modifier = Modifier.weight(1f)) {
                month = nextMonth
            }
        }
        FieldLabel("Category (optional)", top = 12.dp)
        CategoryField(category = categories.find { it.id == categoryId }, onClick = { dismissKeyboardThen { showCategoryPicker = true } })
        DialogButtons(
            cta = if (initial == null) "Add" else "Save",
            onCancel = onDismiss,
            onConfirm = { if (valid) onSave(note.trim(), parsedAmount, month.atDay(1), categoryId) else attempted = true },
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
    if (showCategoryPicker) {
        CategoryPickerSheet(
            flow = FlowType.EXPENSE,
            categories = categories,
            selectedId = categoryId,
            onPick = { categoryId = it?.id },
            onDismiss = { showCategoryPicker = false },
            onAddCategory = onAddCategory,
        )
    }
}

/** "Mark Done & Add Expense" / "Add Expense": amount + category prefilled from the forecast (editable), an actual-expense date defaulting to today, optional note. */
@Composable
private fun ForecastExpenseConfirmDialog(
    forecast: ForecastEntity,
    currencySymbol: String,
    categories: List<CategoryEntity>,
    onConfirm: (amount: Double, categoryId: Long, date: LocalDate, note: String) -> Unit,
    onDismiss: () -> Unit,
    onAddCategory: () -> Unit = {},
) {
    val colors = SyncSpendTheme.colors
    var amountText by remember { mutableStateOf(trimAmount(forecast.amount)) }
    var categoryId by remember { mutableStateOf(forecast.categoryId) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }
    val dismissKeyboardThen = rememberDismissKeyboardThen()
    val parsedAmount = amountText.toDoubleOrNull()
    val valid = parsedAmount != null && parsedAmount > 0 && categoryId != null

    DesignDialog(onDismiss = onDismiss) {
        DialogHeader(
            icon = SyncSpendIcons.Cal,
            iconTint = colors.ink,
            title = "Add the actual expense",
            body = "Confirm what was really spent for “${forecast.note}”.",
        )
        FieldLabel("Amount", top = 14.dp)
        DesignTextField(
            value = amountText,
            onValueChange = { v -> if (v.matches(Regex("""\d*\.?\d{0,2}"""))) amountText = v },
            placeholder = currencySymbol + "0",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            error = attempted && (parsedAmount == null || parsedAmount <= 0),
        )
        FieldLabel("Category", top = 12.dp)
        CategoryField(category = categories.find { it.id == categoryId }, onClick = { dismissKeyboardThen { showCategoryPicker = true } })
        if (attempted && categoryId == null) {
            Text("Pick a category.", style = MaterialTheme.typography.labelSmall, color = colors.neg, modifier = Modifier.padding(top = 4.dp))
        }
        FieldLabel("Date", top = 12.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.tile, RoundedCornerShape(14.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { dismissKeyboardThen { showDatePicker = true } })
                .padding(horizontal = 13.dp, vertical = 12.dp),
        ) { Text(DateUtils.shortDateYear(date), style = MaterialTheme.typography.bodyMedium, color = colors.ink) }
        FieldLabel("Note (optional)", top = 12.dp)
        DesignTextField(value = note, onValueChange = { note = it }, placeholder = "Add a note")
        DialogButtons(
            cta = "Add Expense",
            onCancel = onDismiss,
            onConfirm = { if (valid) onConfirm(parsedAmount, categoryId!!, date, note.trim()) else attempted = true },
        )
    }
    if (showCategoryPicker) {
        CategoryPickerSheet(
            flow = FlowType.EXPENSE,
            categories = categories,
            selectedId = categoryId,
            onPick = { categoryId = it?.id },
            onDismiss = { showCategoryPicker = false },
            onAddCategory = onAddCategory,
        )
    }
    if (showDatePicker) {
        DatePickerSheet(initial = date, onApply = { date = it; showDatePicker = false }, onDismiss = { showDatePicker = false })
    }
}

@Composable
private fun FieldLabel(text: String, top: Dp) {
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
