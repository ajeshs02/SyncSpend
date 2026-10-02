package com.ajesh.syncspend.ui.addentry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.domain.model.CalendarBounds
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.model.ContributionKind
import com.ajesh.syncspend.domain.model.EntryNote
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.FundingSource
import com.ajesh.syncspend.ui.components.AmountEntryPad
import com.ajesh.syncspend.ui.components.CategoryField
import com.ajesh.syncspend.ui.components.ConfirmDialog
import com.ajesh.syncspend.ui.components.DatePickerSheet
import com.ajesh.syncspend.ui.components.DesignTextField
import com.ajesh.syncspend.ui.components.FlowToggle
import com.ajesh.syncspend.ui.components.PickerChip
import com.ajesh.syncspend.ui.components.PrimaryButton
import com.ajesh.syncspend.ui.components.CategoryPickerSheet
import com.ajesh.syncspend.ui.components.SquareIconButton
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import java.time.YearMonth

@Composable
fun AddEntryScreen(onBack: () -> Unit, onSaved: () -> Unit, onAddCategory: (FlowType) -> Unit = {}) {
    val container = LocalAppContainer.current
    val viewModel: AddEntryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AddEntryViewModel(
                    container.transactionRepository,
                    container.categoryRepository,
                    container.preferencesRepository,
                    container.selectionState,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SyncSpendTheme.colors
    var showDatePicker by remember { mutableStateOf(false) }
    // Held here (not in the ViewModel) so typing is never delayed by a state round-trip.
    var note by rememberSaveable { mutableStateOf("") }
    var hint by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val earliest by container.transactionRepository.getEarliestDate().collectAsStateWithLifecycle(null)
    val draftColor = com.ajesh.syncspend.ui.components.flowColor(state.type, colors)
    var pendingOverdraftConfirm by remember { mutableStateOf(false) }

    // The "New Entry" heading row stays fixed; everything below it sits in the (only just
    // scrollable) region under it, so nothing can scroll up under the status bar.
    Column(modifier = Modifier.fillMaxSize().padding(top = SyncSpendChrome.screenTopInset)) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SquareIconButton(
                SyncSpendIcons.Back, onBack, size = 36.dp, radius = 13.dp, iconSize = 17.dp,
                background = colors.pill,
                modifier = Modifier.border(1.dp, colors.line, RoundedCornerShape(13.dp)),
            )
            Text("New Entry", style = MaterialTheme.typography.titleLarge, color = colors.ink)
        }

        // imePadding: with the keyboard up (typing a note) the scroll region shrinks and keeps the field in view.
        BoxWithConstraints(modifier = Modifier.weight(1f).imePadding()) {
            val minHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = minHeight),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    FlowToggle(
                        type = state.type,
                        onSelect = viewModel::setType,
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp),
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(draftColor, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                com.ajesh.syncspend.ui.components.labelFor(state.type).uppercase(),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.84.sp,
                                color = draftColor,
                            )
                        }
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(state.currencySymbol, fontSize = 24.sp, fontWeight = FontWeight.Normal, color = draftColor)
                            Text(
                                CurrencyFormatter.groupTyped(state.amountText),
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-1.3).sp,
                                color = colors.ink,
                            )
                        }
                        Box(
                            Modifier
                                .padding(top = 12.dp)
                                .width(88.dp)
                                .height(2.dp)
                                .background(draftColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
                        )
                        Text(
                            DateUtils.longDate(state.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.sub,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }

                    DesignTextField(
                        value = note,
                        onValueChange = { if (EntryNote.accepts(note, it)) note = it },
                        placeholder = "Add a note (optional)",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp),
                    )

                    CategoryField(
                        category = state.selectedCategory,
                        onClick = viewModel::openCategoryPicker,
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp),
                    )

                    val typedAmount = state.amountText.toDoubleOrNull() ?: 0.0
                    if (state.type == FlowType.EXPENSE) {
                        FundingSourceRow(
                            source = state.fundingSource,
                            onSelect = viewModel::setFundingSource,
                            savingsBalanceAfter = state.savingsBalance - typedAmount,
                            currencySymbol = state.currencySymbol,
                            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp),
                        )
                    }
                    if (state.type == FlowType.SAVINGS) {
                        ContributionKindRow(
                            kind = state.contributionKind,
                            onSelect = viewModel::setContributionKind,
                            regularBalanceAfter = state.regularFundsBalance - typedAmount,
                            currencySymbol = state.currencySymbol,
                            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = SyncSpendChrome.screenBottomContentPadding + 18.dp),
                ) {
                    hint?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.neg,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    AmountEntryPad(
                        dateLabel = DateUtils.shortDate(state.date),
                        onDigit = { hint = null; viewModel.pressDigit(it) },
                        onBackspace = viewModel::pressBackspace,
                        onDateClick = { showDatePicker = true },
                        showDecimalKey = state.allowDecimalInput,
                    )
                    PrimaryButton(
                        text = "Save Entry",
                        onClick = {
                            val typedAmount = state.amountText.toDoubleOrNull() ?: 0.0
                            // A transfer that would take Regular funds negative is allowed, not blocked —
                            // it just needs one extra confirmation tap first (see the plan's accounting
                            // model: the save itself never changes based on this, only whether the user is
                            // warned beforehand).
                            val wouldOverdraw = state.type == FlowType.SAVINGS &&
                                state.contributionKind == ContributionKind.TRANSFER &&
                                state.regularFundsBalance - typedAmount < 0.0
                            if (wouldOverdraw) {
                                pendingOverdraftConfirm = true
                            } else {
                                viewModel.save(
                                    note,
                                    onDone = {
                                        // Tab state is saved and restored, so the next New Entry must not come back with this note.
                                        note = ""
                                        onSaved()
                                    },
                                    onInvalidAmount = { hint = "Enter an amount." },
                                )
                            }
                        },
                        modifier = Modifier.padding(top = 10.dp).fillMaxWidth(),
                        leading = {
                            // Flow dot tinted to read on the button (same tints the toggle's icons use).
                            val onButton = if (state.type == FlowType.EXPENSE) colors.expenseOnSelected else colors.brand
                            Box(Modifier.size(7.dp).background(onButton, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(8.dp))
                        },
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerSheet(
            initial = state.date,
            minMonth = CalendarBounds.entryLowerMonth(YearMonth.now(), earliest),
            onApply = viewModel::setDate,
            onDismiss = { showDatePicker = false },
        )
    }

    if (state.categoryPickerOpen) {
        CategoryPickerSheet(
            flow = state.type,
            categories = state.categoriesForType,
            selectedId = state.selectedCategory?.id,
            onPick = { cat ->
                if (cat == null) viewModel.clearCategory() else viewModel.selectCategory(cat)
            },
            onDismiss = viewModel::closeCategoryPicker,
            onAddCategory = { onAddCategory(state.type) },
        )
    }

    if (pendingOverdraftConfirm) {
        val afterTransfer = state.regularFundsBalance - (state.amountText.toDoubleOrNull() ?: 0.0)
        ConfirmDialog(
            title = "Transfer more than you have?",
            body = "This transfer would take your regular funds below zero (to " +
                (if (afterTransfer < 0) "-" else "") + state.currencySymbol + CurrencyFormatter.amount(afterTransfer) +
                "). The transfer still moves the money into savings exactly as entered — this only confirms you meant to.",
            cta = "Transfer anyway",
            onConfirm = {
                pendingOverdraftConfirm = false
                viewModel.save(
                    note,
                    onDone = { note = ""; onSaved() },
                    onInvalidAmount = { hint = "Enter an amount." },
                )
            },
            onDismiss = { pendingOverdraftConfirm = false },
        )
    }
}

/** Expense-only: which pool of money paid for it. See [AnalyticsEngine.savingsBalance]'s doc for the formula. */
@Composable
private fun FundingSourceRow(
    source: FundingSource,
    onSelect: (FundingSource) -> Unit,
    savingsBalanceAfter: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
) {
    val colors = SyncSpendTheme.colors
    Column(modifier = modifier) {
        Text("Funding source", style = MaterialTheme.typography.labelSmall, color = colors.sub)
        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PickerChip(
                label = "Regular funds",
                selected = source == FundingSource.REGULAR,
                modifier = Modifier.weight(1f),
                vertical = 10.dp,
            ) { onSelect(FundingSource.REGULAR) }
            PickerChip(
                label = "Savings",
                selected = source == FundingSource.SAVINGS,
                modifier = Modifier.weight(1f),
                vertical = 10.dp,
            ) { onSelect(FundingSource.SAVINGS) }
        }
        if (source == FundingSource.SAVINGS) {
            Text(
                "Savings: ${if (savingsBalanceAfter < 0) "-" else ""}$currencySymbol${CurrencyFormatter.amount(savingsBalanceAfter)} after this",
                style = MaterialTheme.typography.labelSmall,
                color = if (savingsBalanceAfter < 0) colors.neg else colors.sub,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** Savings-only: whether this is genuinely new money or a reallocation of income already counted. */
@Composable
private fun ContributionKindRow(
    kind: ContributionKind,
    onSelect: (ContributionKind) -> Unit,
    regularBalanceAfter: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
) {
    val colors = SyncSpendTheme.colors
    Column(modifier = modifier) {
        Text("Where is this money coming from?", style = MaterialTheme.typography.labelSmall, color = colors.sub)
        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PickerChip(
                label = "New income",
                selected = kind == ContributionKind.NEW_INCOME,
                modifier = Modifier.weight(1f),
                vertical = 10.dp,
            ) { onSelect(ContributionKind.NEW_INCOME) }
            PickerChip(
                label = "Transfer existing",
                selected = kind == ContributionKind.TRANSFER,
                modifier = Modifier.weight(1f),
                vertical = 10.dp,
            ) { onSelect(ContributionKind.TRANSFER) }
        }
        if (kind == ContributionKind.TRANSFER) {
            Text(
                "Regular funds: ${if (regularBalanceAfter < 0) "-" else ""}$currencySymbol${CurrencyFormatter.amount(regularBalanceAfter)} after this",
                style = MaterialTheme.typography.labelSmall,
                color = if (regularBalanceAfter < 0) colors.neg else colors.sub,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
