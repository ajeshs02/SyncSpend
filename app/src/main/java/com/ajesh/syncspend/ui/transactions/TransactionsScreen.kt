package com.ajesh.syncspend.ui.transactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.components.AnimatedSegmentedControl
import com.ajesh.syncspend.ui.components.DateRangePickerSheet
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils

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
                    container.selectionState,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Pure UI state lives here, not in the ViewModel: toggling a menu must not re-run the data pipeline.
    var flowMenuOpen by remember { mutableStateOf(false) }
    var rangePickerOpen by remember { mutableStateOf(false) }
    val colors = SyncSpendTheme.colors
    val flowColor by animateColorAsState(if (state.flow == FlowType.INCOME) colors.pos else colors.neg, tween(200), label = "flow-word")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = SyncSpendChrome.screenTopInset)
            .padding(horizontal = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Transactions", style = MaterialTheme.typography.headlineSmall, color = SyncSpendTheme.colors.ink)
            Box(modifier = Modifier.weight(1f))
            Box {
                Row(
                    modifier = Modifier
                        .background(SyncSpendTheme.colors.card, RoundedCornerShape(14.dp))
                        .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { flowMenuOpen = !flowMenuOpen },
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (state.flow == FlowType.INCOME) SyncSpendIcons.ArrowIn else SyncSpendIcons.ArrowOut,
                        null,
                        tint = if (state.flow == FlowType.INCOME) Color(0xFF8ECF63) else SyncSpendTheme.colors.neg,
                        modifier = Modifier.size(15.dp),
                    )
                    Box(Modifier.width(5.dp))
                    Text(
                        if (state.flow == FlowType.INCOME) "Income" else "Expense",
                        style = MaterialTheme.typography.labelLarge,
                        color = SyncSpendTheme.colors.ink,
                    )
                }
                if (flowMenuOpen) {
                    FlowMenuPopup(
                        current = state.flow,
                        onPick = {
                            viewModel.setFlow(it)
                            flowMenuOpen = false
                        },
                        onDismiss = { flowMenuOpen = false },
                    )
                }
            }
        }

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 10.dp))
        Text(
            buildAnnotatedString {
                append("Showing ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = flowColor)) {
                    append(if (state.flow == FlowType.INCOME) "income" else "expenses")
                }
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = colors.sub,
        )

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 14.dp))
        AnimatedSegmentedControl(
            options = listOf("Entries", "Categories", "Stats"),
            selectedIndex = state.tab.ordinal,
            onSelect = { viewModel.selectTab(TransactionsTab.entries[it]) },
        )

        if (state.tab != TransactionsTab.ANALYTICS) {
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 14.dp))
            FilterChipsRow(
                options = state.entryFilterOptions,
                selected = state.entryFilter,
                customLabel = state.customRange?.let { DateUtils.rangeLabel(it.start, it.end) },
                onSelect = { filter ->
                    if (filter == EntryFilter.CUSTOM) rangePickerOpen = true else viewModel.pickEntryFilter(filter)
                },
            )
        }

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 10.dp))
        when (state.tab) {
            TransactionsTab.ENTRIES -> EntriesTab(
                groups = state.dayGroups,
                onRowClick = { container.selectionState.editingTransactionId.value = it },
                modifier = Modifier.fillMaxWidth(),
            )
            TransactionsTab.CATEGORIES -> CategoriesTab(rollups = state.categoryRollups, modifier = Modifier.fillMaxWidth())
            TransactionsTab.ANALYTICS -> state.stats?.let { StatsTab(it, modifier = Modifier.fillMaxWidth()) }
        }
    }

    if (rangePickerOpen) {
        DateRangePickerSheet(
            initial = state.customRange,
            earliestTransactionDate = state.earliestTransactionDate,
            onApply = viewModel::applyCustomRange,
            onDismiss = { rangePickerOpen = false },
        )
    }
}

@Composable
private fun FlowMenuPopup(current: FlowType, onPick: (FlowType) -> Unit, onDismiss: () -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val visible = remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
    androidx.compose.ui.window.Popup(
        alignment = Alignment.TopEnd,
        offset = androidx.compose.ui.unit.IntOffset(0, with(density) { 42.dp.roundToPx() }),
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true),
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visibleState = visible,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(140)) +
                androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(140), initialScale = 0.92f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)),
        ) {
            Column(
                modifier = Modifier
                    .shadow(14.dp, RoundedCornerShape(14.dp))
                    .background(SyncSpendTheme.colors.sheet, RoundedCornerShape(14.dp))
                    .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(14.dp))
                    .padding(6.dp)
                    .widthIn(min = 126.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FlowMenuItem("Expense", FlowType.EXPENSE, current, onPick)
                FlowMenuItem("Income", FlowType.INCOME, current, onPick)
            }
        }
    }
}

@Composable
private fun FlowMenuItem(label: String, value: FlowType, current: FlowType, onPick: (FlowType) -> Unit) {
    val colors = SyncSpendTheme.colors
    val selected = value == current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.selectedBrush else androidx.compose.ui.graphics.SolidColor(Color.Transparent), RoundedCornerShape(10.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPick(value) }
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            if (value == FlowType.INCOME) SyncSpendIcons.ArrowIn else SyncSpendIcons.ArrowOut,
            null,
            tint = if (value == FlowType.INCOME) Color(0xFF8ECF63) else if (selected) colors.expenseOnSelected else colors.neg,
            modifier = Modifier.size(14.dp),
        )
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) colors.onSelected else colors.ink)
    }
}
