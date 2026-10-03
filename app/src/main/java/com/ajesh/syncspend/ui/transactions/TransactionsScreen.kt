package com.ajesh.syncspend.ui.transactions

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.model.TransactionsTab
import com.ajesh.syncspend.ui.components.AnimatedSegmentedControl
import com.ajesh.syncspend.ui.components.ChipsRow
import com.ajesh.syncspend.ui.components.PeriodPickerSheet
import com.ajesh.syncspend.ui.components.SegmentIcon
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun TransactionsScreen() {
    val container = LocalAppContainer.current
    val viewModel: TransactionsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TransactionsViewModel(
                    container.transactionRepository,
                    container.categoryRepository,
                    container.preferencesRepository,
                    container.subscriptionRepository,
                    container.transferRepository,
                    container.selectionState,
                )
            }
        },
    )
    // Selections are cheap main-thread state, so a tap moves its pill at once; each tab's rows
    // come from their own flow, collected only while that tab is showing.
    val viewFilter by viewModel.viewFilter.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val scope by viewModel.scope.collectAsStateWithLifecycle()
    val earliestDate by viewModel.earliestDate.collectAsStateWithLifecycle()
    var periodPickerOpen by remember { mutableStateOf(false) }
    val colors = SyncSpendTheme.colors

    // Round 11: back to a compact chip row (was the pill+arrows from round 10) — This/Last Month are
    // specific ScopePeriod.Month values applied straight to the shared scope; anything else selects
    // "Custom" and opens the same PeriodPickerSheet below, unchanged since round 10.
    val today = remember { LocalDate.now() }
    val thisMonthScope = remember(today) { ScopePeriod.Month(YearMonth.from(today)) }
    val lastMonthScope = remember(today) { ScopePeriod.Month(YearMonth.from(today).minusMonths(1)) }
    val chipIndex = when (scope) {
        thisMonthScope -> 0
        lastMonthScope -> 1
        else -> 2
    }
    val resolvedRange = remember(scope, earliestDate) { AnalyticsEngine.scopeRange(scope, today, earliestDate) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = SyncSpendChrome.screenTopInset)
            .padding(horizontal = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Transactions",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 23.sp),
                color = SyncSpendTheme.colors.ink,
                modifier = Modifier.padding(vertical = 3.dp),
            )
            Box(modifier = Modifier.weight(1f))
            TransactionsFlowMenuToggle(current = viewFilter, onPick = viewModel::pickViewFilter)
        }

        val tabIcons = remember(colors) {
            listOf(SyncSpendIcons.Receipt, SyncSpendIcons.Layers)
                .map { SegmentIcon(it, colors.onSelected, colors.sub) }
        }
        AnimatedSegmentedControl(
            options = listOf("Entries", "Categories"),
            selectedIndex = tab.ordinal,
            onSelect = { viewModel.selectTab(TransactionsTab.entries[it]) },
            icons = tabIcons,
            iconSize = 14.dp,
            modifier = Modifier.padding(top = 14.dp),
        )

        // Round 12: the period filter now sits below the Entries/Categories toggle (was above it) —
        // same chips, same shared scope, same Custom -> PeriodPickerSheet behavior, just reordered.
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
        ChipsRow(
            labels = listOf("This Month", "Last Month", "Custom"),
            selectedIndex = chipIndex,
            onSelect = { i ->
                when (i) {
                    0 -> viewModel.applyScope(thisMonthScope)
                    1 -> viewModel.applyScope(lastMonthScope)
                    else -> periodPickerOpen = true
                }
            },
        )
        Text(
            DateUtils.appliedRangeLabel(resolvedRange),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = colors.sub,
            modifier = Modifier.padding(top = 8.dp),
        )

        // The gap under the chips sits outside the scrolling list, so scrolled rows/cards clip a
        // clear band below them instead of running flush against them.
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 14.dp))
        // Only the visible tab is collected, so only its rows are ever built. Until its first
        // state arrives nothing is drawn (never a flash of "No transactions…").
        when (tab) {
            TransactionsTab.ENTRIES -> {
                val entries by viewModel.entries.collectAsStateWithLifecycle()
                entries?.let {
                    EntriesTab(
                        groups = it.groups,
                        onRowClick = { id ->
                            // A Savings-filter row is a TransferEntity, never a TransactionEntity — the
                            // ids come from different tables and must not be looked up through the
                            // wrong sheet (see SharedSelectionState.editingTransferId's doc).
                            if (viewFilter == TransactionsFlowFilter.SAVINGS) container.selectionState.editingTransferId.value = id
                            else container.selectionState.editingTransactionId.value = id
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            TransactionsTab.CATEGORIES -> {
                val rollups by viewModel.rollups.collectAsStateWithLifecycle()
                rollups?.let { CategoriesTab(rollups = it.rollups, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }

    if (periodPickerOpen) {
        PeriodPickerSheet(
            currentScope = scope,
            earliestTransactionDate = earliestDate,
            showAllTime = true,
            onApply = viewModel::applyScope,
            onDismiss = { periodPickerOpen = false },
        )
    }
}

/**
 * "on 21 Sep", "from 21 Sep to 27 Sep", or null before a custom range has been picked. No longer
 * called by this screen itself (round 10 replaced the EntryFilter-chip header sentence with the
 * period pill's own label) but kept — [EntryFilter]/[AnalyticsEngine.entryFilterRange] are both still
 * very much alive (the Savings/Transfer screen's own date filter), and this is still exercised
 * directly by `CalendarBoundsTest`.
 */
internal fun headerDateClause(filter: EntryFilter, customRange: DateRange?, today: LocalDate): String? {
    fun clause(range: DateRange): String {
        fun label(d: LocalDate) = DateUtils.smartDate(d, today)
        return if (range.start == range.end) "on ${label(range.start)}" else "from ${label(range.start)} to ${label(range.end)}"
    }
    return AnalyticsEngine.entryFilterRange(filter, customRange, today)?.let(::clause)
}

/**
 * The Transactions page's own 3-way pill — [TransactionsFlowFilter] isn't a [FlowType], so this
 * doesn't retrofit the shared [com.ajesh.syncspend.ui.components.FlowMenuToggle] popup (used
 * elsewhere for the real 2-option Expense/Income case); it's a small, focused copy of that same
 * compact-pill-with-dropdown pattern for this screen's one extra option.
 */
@Composable
private fun TransactionsFlowMenuToggle(current: TransactionsFlowFilter, onPick: (TransactionsFlowFilter) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val colors = SyncSpendTheme.colors
    Box {
        Row(
            modifier = Modifier
                .background(colors.selectedBrush, RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { open = !open },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                viewFilterIcon(current),
                null,
                tint = if (current == TransactionsFlowFilter.EXPENSE) colors.expenseOnSelected else colors.brand,
                modifier = Modifier.size(15.dp),
            )
            Box(Modifier.size(5.dp))
            Text(viewFilterTitle(current), style = MaterialTheme.typography.labelLarge, color = colors.onSelected)
        }
        if (open) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, with(LocalDensity.current) { 42.dp.roundToPx() }),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                Column(
                    modifier = Modifier
                        .background(colors.sheet, RoundedCornerShape(14.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                        .padding(6.dp)
                        .width(150.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TransactionsFlowFilter.entries.forEach { value ->
                        val selected = value == current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) colors.selectedBrush else SolidColor(Color.Transparent),
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onPick(value); open = false }
                                .padding(horizontal = 11.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                viewFilterIcon(value),
                                null,
                                tint = when {
                                    value != TransactionsFlowFilter.EXPENSE -> colors.brand
                                    selected -> colors.expenseOnSelected
                                    else -> colors.neg
                                },
                                modifier = Modifier.size(14.dp),
                            )
                            Text(viewFilterTitle(value), style = MaterialTheme.typography.labelLarge, color = if (selected) colors.onSelected else colors.ink)
                        }
                    }
                }
            }
        }
    }
}

private fun viewFilterTitle(filter: TransactionsFlowFilter): String = when (filter) {
    TransactionsFlowFilter.EXPENSE -> "Expense"
    TransactionsFlowFilter.INCOME -> "Income"
    TransactionsFlowFilter.SAVINGS -> "Savings"
}

private fun viewFilterIcon(filter: TransactionsFlowFilter) = when (filter) {
    TransactionsFlowFilter.EXPENSE -> SyncSpendIcons.ArrowOut
    TransactionsFlowFilter.INCOME -> SyncSpendIcons.ArrowIn
    TransactionsFlowFilter.SAVINGS -> SyncSpendIcons.Coin
}
