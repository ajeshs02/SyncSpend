package com.ajesh.syncspend.ui.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransferEntity
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.model.TransferDirection
import com.ajesh.syncspend.ui.components.AnimatedSegmentedControl
import com.ajesh.syncspend.ui.components.CategoryField
import com.ajesh.syncspend.ui.components.CategoryPickerSheet
import com.ajesh.syncspend.ui.components.ConfirmDialog
import com.ajesh.syncspend.ui.components.DatePickerSheet
import com.ajesh.syncspend.ui.components.DesignDialog
import com.ajesh.syncspend.ui.components.DesignTextField
import com.ajesh.syncspend.ui.components.DialogButtons
import com.ajesh.syncspend.ui.components.DialogHeader
import com.ajesh.syncspend.ui.components.HeaderAddButton
import com.ajesh.syncspend.ui.components.PeriodPickerSheet
import com.ajesh.syncspend.ui.components.rememberDismissKeyboardThen
import com.ajesh.syncspend.ui.components.SheetDeleteButton
import com.ajesh.syncspend.ui.components.SheetHeader
import com.ajesh.syncspend.ui.components.DesignSheet
import com.ajesh.syncspend.ui.components.SquareIconButton
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.ui.transactions.FilterChipsRow
import com.ajesh.syncspend.ui.transactions.FlatTxList
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun TransferScreen(onBack: () -> Unit, onAddCategory: (FlowType) -> Unit = {}) {
    val container = LocalAppContainer.current
    val viewModel: TransferViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TransferViewModel(
                    container.transferRepository,
                    container.categoryRepository,
                    container.preferencesRepository,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SyncSpendTheme.colors
    var showAdd by remember { mutableStateOf(false) }
    var rangePickerOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(top = SyncSpendChrome.screenTopInset).padding(horizontal = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SquareIconButton(
                SyncSpendIcons.Back, onBack, size = 36.dp, radius = 13.dp, iconSize = 17.dp,
                background = colors.pill,
                modifier = Modifier.border(1.dp, colors.line, RoundedCornerShape(13.dp)),
            )
            Text("Savings", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = colors.ink, modifier = Modifier.weight(1f))
            HeaderAddButton("Transfer", icon = SyncSpendIcons.Swap, onClick = { showAdd = true })
        }
        Text(
            "Track money added to and withdrawn from savings",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = colors.sub,
            modifier = Modifier.padding(top = 2.dp),
        )

        Column(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .background(colors.mintGradient, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Text("Savings Balance", fontSize = 11.sp, color = colors.mink.copy(alpha = 0.7f))
            Text(
                (if (state.savingsBalance < 0) "-" else "") + state.currencySymbol + CurrencyFormatter.amount(state.savingsBalance),
                fontSize = 21.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                // Flagged persistently (not just at the moment of the triggering edit) for as long
                // as the balance stays negative — same colors.neg-on-gradient treatment Home's hero
                // card already uses for a bad trend against its own accent background.
                color = if (state.savingsBalance < 0) colors.neg else colors.mink,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        FilterChipsRow(
            options = listOf(EntryFilter.THIS_MONTH, EntryFilter.LAST_MONTH, EntryFilter.CUSTOM),
            selected = state.entryFilter,
            customLabel = state.customRange?.let { DateUtils.rangeLabel(it.start, it.end) },
            onSelect = { f -> if (f == EntryFilter.CUSTOM) rangePickerOpen = true else viewModel.pickEntryFilter(f) },
            modifier = Modifier.padding(top = 16.dp),
        )

        FlatTxList(
            rows = state.rows,
            onRowClick = { id -> container.selectionState.editingTransferId.value = id },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }

    if (showAdd) {
        TransferFormDialog(
            initial = null,
            savingsBalanceBefore = state.savingsBalance,
            currencySymbol = state.currencySymbol,
            categories = state.incomeCategories,
            onSave = { direction, amount, date, categoryId, note ->
                viewModel.add(amount, direction, date, categoryId, note)
                showAdd = false
            },
            onDelete = null,
            onDismiss = { showAdd = false },
            onAddCategory = { onAddCategory(FlowType.INCOME) },
        )
    }
    if (rangePickerOpen) {
        // Always seeds fresh at "this month" — Savings never persisted a "last picked shape" for
        // Custom either, only the literal From/To dates this picker doesn't have a slot for.
        PeriodPickerSheet(
            currentScope = ScopePeriod.Month(YearMonth.now()),
            earliestTransactionDate = state.earliestTransferDate,
            showAllTime = true,
            onApply = { scope ->
                viewModel.applyCustomRange(AnalyticsEngine.scopeRange(scope, LocalDate.now(), state.earliestTransferDate))
            },
            onDismiss = { rangePickerOpen = false },
        )
    }
}

/** Hosted once at the app root (see [com.ajesh.syncspend.ui.editentry.EditEntryHost]'s analogous pattern) so it paints above the floating nav, for whichever transfer [com.ajesh.syncspend.domain.state.SharedSelectionState.editingTransferId] names. */
@Composable
fun EditTransferHost(onAddCategory: (FlowType) -> Unit = {}) {
    val container = LocalAppContainer.current
    val id by container.selectionState.editingTransferId.collectAsStateWithLifecycle()
    id?.let {
        EditTransferSheet(
            it,
            onDismiss = { container.selectionState.editingTransferId.value = null },
            onAddCategory = onAddCategory,
        )
    }
}

@Composable
private fun EditTransferSheet(transferId: Long, onDismiss: () -> Unit, onAddCategory: (FlowType) -> Unit = {}) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val transfer by produceState<TransferEntity?>(null, transferId) {
        value = container.transferRepository.getAll().first().find { it.id == transferId }
    }
    val categories by container.categoryRepository.getAllByType(FlowType.INCOME).collectAsStateWithLifecycle(emptyList())
    val transfers by container.transferRepository.getAll().collectAsStateWithLifecycle(emptyList())
    val prefs by container.preferencesRepository.preferences.collectAsStateWithLifecycle(
        com.ajesh.syncspend.data.datastore.UserPreferences(),
    )
    val loaded = transfer ?: return
    val balanceBeforeThis = AnalyticsEngine.savingsBalance(transfers) -
        (if (loaded.direction == TransferDirection.TO_SAVINGS) loaded.amount else -loaded.amount)
    var showDelete by remember { mutableStateOf(false) }

    DesignSheet(onDismiss = onDismiss) { close ->
        TransferFormDialogBody(
            initial = loaded,
            currencySymbol = prefs.currencyCode.symbol,
            categories = categories,
            savingsBalanceBefore = balanceBeforeThis,
            isSheet = true,
            onSave = { direction, amount, date, categoryId, note ->
                scope.launch {
                    container.transferRepository.update(
                        loaded.copy(direction = direction, amount = amount, date = date, categoryId = categoryId.takeIf { direction == TransferDirection.TO_SAVINGS }, note = note),
                    )
                    close()
                }
            },
            onDelete = { showDelete = true },
            onCancel = close,
            onAddCategory = { onAddCategory(FlowType.INCOME) },
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = "Delete this transfer?",
            body = "Removing this ${prefs.currencyCode.symbol}${CurrencyFormatter.amount(loaded.amount)} transfer cannot be undone.",
            cta = "Delete",
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                scope.launch { container.transferRepository.delete(loaded); onDismiss() }
            },
        )
    }
}

/** The Add-transfer dialog (modal, via [DesignDialog]) and the Edit-transfer sheet both render this body. */
@Composable
private fun TransferFormDialog(
    initial: TransferEntity?,
    currencySymbol: String,
    categories: List<CategoryEntity>,
    savingsBalanceBefore: Double,
    onSave: (direction: TransferDirection, amount: Double, date: LocalDate, categoryId: Long?, note: String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    onAddCategory: () -> Unit = {},
) {
    DesignDialog(onDismiss = onDismiss) {
        TransferFormDialogBody(
            initial = initial,
            currencySymbol = currencySymbol,
            categories = categories,
            savingsBalanceBefore = savingsBalanceBefore,
            isSheet = false,
            onSave = onSave,
            onDelete = onDelete,
            onCancel = onDismiss,
            onAddCategory = onAddCategory,
        )
    }
}

@Composable
private fun TransferFormDialogBody(
    initial: TransferEntity?,
    currencySymbol: String,
    categories: List<CategoryEntity>,
    savingsBalanceBefore: Double,
    isSheet: Boolean,
    onSave: (direction: TransferDirection, amount: Double, date: LocalDate, categoryId: Long?, note: String) -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit,
    onAddCategory: () -> Unit = {},
) {
    val colors = SyncSpendTheme.colors
    // The direction toggle lives inside this shared form body (not the main screen) so both Add and
    // Edit can freely switch it; amount/date/note/category are kept regardless of which way it's flipped.
    var direction by remember { mutableStateOf(initial?.direction ?: TransferDirection.TO_SAVINGS) }
    var amountText by remember { mutableStateOf(initial?.let { trimAmount(it.amount) } ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: LocalDate.now()) }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dismissKeyboardThen = rememberDismissKeyboardThen()
    var attempted by remember { mutableStateOf(false) }
    var pendingNegativeConfirm by remember { mutableStateOf(false) }
    val parsedAmount = amountText.toDoubleOrNull()
    val valid = parsedAmount != null && parsedAmount > 0
    val isContribution = direction == TransferDirection.TO_SAVINGS
    val accent = if (isContribution) colors.pos else colors.neg
    val balanceAfter = savingsBalanceBefore + (if (isContribution) (parsedAmount ?: 0.0) else -(parsedAmount ?: 0.0))

    // Add to Savings dropped its Note field (3a) — never persist a note typed while Withdraw was
    // selected if the user flips back to Add to Savings before saving.
    val noteToSave = if (isContribution) "" else note.trim()

    fun attemptSave() {
        if (!valid) {
            attempted = true
            return
        }
        if (balanceAfter < 0) pendingNegativeConfirm = true
        else onSave(direction, parsedAmount, date, categoryId, noteToSave)
    }

    Column {
        if (isSheet) {
            SheetHeader(if (isContribution) "Add to Savings" else "Withdraw from Savings", onClose = onCancel)
        } else {
            DialogHeader(
                icon = SyncSpendIcons.Coin,
                iconTint = accent,
                title = if (isContribution) "Add to Savings" else "Withdraw from Savings",
                body = if (isContribution) "Record money moved into savings." else "Record money taken out of savings.",
            )
        }
        AnimatedSegmentedControl(
            options = listOf("Add to Savings", "Withdraw"),
            selectedIndex = if (isContribution) 0 else 1,
            onSelect = { direction = if (it == 0) TransferDirection.TO_SAVINGS else TransferDirection.FROM_SAVINGS },
            modifier = Modifier.padding(top = 14.dp),
        )
        FieldLabel("Amount", top = 14.dp)
        DesignTextField(
            value = amountText,
            onValueChange = { v -> if (v.matches(Regex("""\d*\.?\d{0,2}"""))) amountText = v },
            placeholder = currencySymbol + "0",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            error = attempted && (parsedAmount == null || parsedAmount <= 0),
        )
        FieldLabel("Date", top = 12.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.tile, RoundedCornerShape(14.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { dismissKeyboardThen { showDatePicker = true } })
                .padding(horizontal = 13.dp, vertical = 12.dp),
        ) { Text(DateUtils.shortDateYear(date), style = MaterialTheme.typography.bodyMedium, color = colors.ink) }
        if (isContribution) {
            FieldLabel("Source (optional)", top = 12.dp)
            CategoryField(category = categories.find { it.id == categoryId }, onClick = { dismissKeyboardThen { showCategoryPicker = true } })
        } else {
            // Note is Withdraw-only — Add to Savings dropped it to keep that form shorter.
            FieldLabel("Note (optional)", top = 12.dp)
            DesignTextField(value = note, onValueChange = { note = it }, placeholder = "Add a note")
        }

        if (isSheet) {
            Row(modifier = Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                onDelete?.let { SheetDeleteButton(onClick = it) }
                com.ajesh.syncspend.ui.components.PrimaryButton(
                    text = "Save Changes", height = 48.dp, radius = 16.dp, modifier = Modifier.weight(1f), onClick = ::attemptSave,
                )
            }
        } else {
            DialogButtons(cta = if (initial == null) "Add" else "Save", onCancel = onCancel, onConfirm = ::attemptSave)
        }
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            flow = FlowType.INCOME,
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
    if (pendingNegativeConfirm) {
        ConfirmDialog(
            title = "This will go negative",
            body = "Saving this would take the Savings Balance to " +
                (if (balanceAfter < 0) "-" else "") + currencySymbol + CurrencyFormatter.amount(balanceAfter) +
                ". It's saved exactly as entered either way; this only confirms you meant to.",
            cta = "Save anyway",
            destructive = false,
            onConfirm = { pendingNegativeConfirm = false; onSave(direction, parsedAmount!!, date, categoryId, noteToSave) },
            onDismiss = { pendingNegativeConfirm = false },
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

private fun trimAmount(v: Double): String {
    val totalCents = Math.round(v * 100)
    val whole = totalCents / 100
    val cents = totalCents % 100
    return if (cents == 0L) whole.toString() else "$whole.${cents.toString().padStart(2, '0')}"
}
