package com.ajesh.syncspend.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.ui.components.PeriodPickerSheet
import com.ajesh.syncspend.ui.components.PeriodSelectorPill
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate

/**
 * The standalone Stats page (bottom nav), reached instead of a tab inside Transactions. Reuses
 * [TransactionsViewModel] — its `stats` never depended on the Entries/Categories tab, so a second
 * instance here is a clean, minimal reuse rather than a new ViewModel. No more Expense/Income toggle
 * — both flows are built and stacked in one scroll (see [statsItems]). The period picker (round 10) is
 * the same [PeriodPickerSheet] Home uses, sharing one selection across Home/Stats/Transactions via
 * [TransactionsViewModel.scope].
 */
@Composable
fun StatsScreen() {
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
    val scope by viewModel.scope.collectAsStateWithLifecycle()
    val periodNav by viewModel.periodNav.collectAsStateWithLifecycle()
    val earliestDate by viewModel.earliestDate.collectAsStateWithLifecycle()
    val colors = SyncSpendTheme.colors
    var periodPickerOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = SyncSpendChrome.screenTopInset)
            .padding(horizontal = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Stats",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 23.sp),
                color = colors.ink,
                modifier = Modifier.padding(vertical = 3.dp),
            )
        }

        PeriodSelectorPill(
            label = periodNav.label,
            subLabel = null,
            canGoPrev = periodNav.canGoPrev,
            canGoNext = periodNav.canGoNext,
            onPrev = viewModel::prevPeriod,
            onNext = viewModel::nextPeriod,
            onTap = { periodPickerOpen = true },
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            DateUtils.appliedRangeLabel(remember(scope, earliestDate) { AnalyticsEngine.scopeRange(scope, LocalDate.now(), earliestDate) }),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = colors.sub,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.padding(top = 12.dp))
        val stats by viewModel.stats.collectAsStateWithLifecycle()
        stats?.let { (expense, income) ->
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(top = 2.dp, bottom = SyncSpendChrome.screenBottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                item(key = "header-expense") { SectionHeading("Expenses") }
                statsItems(expense, keyPrefix = "exp", includeSavingsCard = true)
                item(key = "income-divider") {
                    Box(Modifier.padding(top = 22.dp).fillMaxWidth().height(1.dp).background(colors.line))
                }
                item(key = "header-income") { SectionHeading("Income") }
                statsItems(income, keyPrefix = "inc", includeSavingsCard = false)
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

@Composable
private fun SectionHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
        color = SyncSpendTheme.colors.ink,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}
