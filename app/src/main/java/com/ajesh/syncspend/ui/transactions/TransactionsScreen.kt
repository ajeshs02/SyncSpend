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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.components.AnimatedSegmentedControl
import com.ajesh.syncspend.ui.components.PeriodPickerSheet
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
                    container.selectionState,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                            onClick = viewModel::toggleFlowMenu,
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (state.flow == FlowType.INCOME) SyncSpendIcons.ArrowIn else SyncSpendIcons.ArrowOut,
                        null,
                        tint = if (state.flow == FlowType.INCOME) Color(0xFF8ECF63) else SyncSpendTheme.colors.neg,
                        modifier = Modifier.padding(end = 5.dp).widthIn(max = 15.dp),
                    )
                    Text(
                        if (state.flow == FlowType.INCOME) "Income" else "Expense",
                        style = MaterialTheme.typography.labelLarge,
                        color = SyncSpendTheme.colors.ink,
                    )
                }
                if (state.flowMenuOpen) {
                    Column(
                        modifier = Modifier
                            .padding(top = 42.dp)
                            .background(SyncSpendTheme.colors.sheet, RoundedCornerShape(14.dp))
                            .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(14.dp))
                            .padding(6.dp),
                    ) {
                        FlowMenuItem("Expense", FlowType.EXPENSE, state.flow, viewModel::setFlow)
                        FlowMenuItem("Income", FlowType.INCOME, state.flow, viewModel::setFlow)
                    }
                }
            }
        }

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 10.dp))
        Text(
            "Showing ${if (state.flow == FlowType.INCOME) "income" else "expenses"} for ${state.scopeLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = SyncSpendTheme.colors.sub,
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
                onSelect = viewModel::pickEntryFilter,
            )
        }

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 10.dp))
        when (state.tab) {
            TransactionsTab.ENTRIES -> EntriesTab(
                groups = state.dayGroups,
                onRowClick = { /* Edit Entry sheet lands in Phase 5 */ },
                modifier = Modifier.fillMaxWidth(),
            )
            TransactionsTab.CATEGORIES -> CategoriesTab(rollups = state.categoryRollups, modifier = Modifier.fillMaxWidth())
            TransactionsTab.ANALYTICS -> Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                Text("Stats — coming in Phase 9", style = MaterialTheme.typography.bodySmall, color = SyncSpendTheme.colors.sub)
            }
        }
    }

    if (state.periodPickerOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        PeriodPickerSheet(
            currentScope = state.currentScope,
            earliestTransactionDate = state.earliestTransactionDate,
            onApply = viewModel::applyScope,
            onDismiss = viewModel::closePeriodPicker,
            sheetState = sheetState,
        )
    }
}

@Composable
private fun FlowMenuItem(label: String, value: FlowType, current: FlowType, onPick: (FlowType) -> Unit) {
    val selected = value == current
    Row(
        modifier = Modifier
            .background(if (selected) SyncSpendTheme.colors.darkGradient else androidx.compose.ui.graphics.SolidColor(Color.Transparent), RoundedCornerShape(10.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPick(value) }
            .padding(horizontal = 11.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else SyncSpendTheme.colors.ink,
        )
    }
}
