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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
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
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.model.TransactionsTab
import com.ajesh.syncspend.ui.components.AnimatedSegmentedControl
import com.ajesh.syncspend.ui.components.ChipsRow
import com.ajesh.syncspend.ui.components.FlowMenuToggle
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
                    container.selectionState,
                )
            }
        },
    )
    // Selections are cheap main-thread state, so a tap moves its pill at once; each tab's rows
    // come from their own flow, collected only while that tab is showing.
    val viewFilter by viewModel.viewFilter.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val scope by viewModel.scope.collectAsStateWithLifecycle()
    val earliestDate by viewModel.earliestDate.collectAsStateWithLifecycle()
    var periodPickerOpen by remember { mutableStateOf(false) }
    var categoryEntriesSheetFor by remember { mutableStateOf<Long?>(null) }
    val colors = SyncSpendTheme.colors

    // Round 11: back to a compact chip row (was the pill+arrows from round 10) — This/Last Month are
    // specific ScopePeriod.Month values applied straight to Transactions' own independent scope;
    // anything else selects "Custom" and opens the same PeriodPickerSheet below, unchanged since round 10.
    val today = remember { LocalDate.now() }
    val thisMonthScope = remember(today) { ScopePeriod.Month(YearMonth.from(today)) }
    val lastMonthScope = remember(today) { ScopePeriod.Month(YearMonth.from(today).minusMonths(1)) }
    val chipIndex = when (scope) {
        thisMonthScope -> 0
        lastMonthScope -> 1
        else -> 2
    }
    val resolvedRange = remember(scope, earliestDate) { AnalyticsEngine.scopeRange(scope, today, earliestDate) }

    // Collected unconditionally (not just while their own tab is showing): `entries`' totalFormatted
    // drives the Overall Total row beside the date-range label regardless of which tab is active, and
    // `rollups` is also needed to resolve the category-entries popup's title reactively.
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val rollups by viewModel.rollups.collectAsStateWithLifecycle()

    val entriesListState = rememberLazyListState()
    val categoriesListState = rememberLazyListState()
    // Date-range change resets both lists to the top.
    LaunchedEffect(resolvedRange) {
        entriesListState.scrollToItem(0)
        categoriesListState.scrollToItem(0)
    }
    // Switching Entries<->Categories resets whichever list just became visible.
    LaunchedEffect(tab) {
        if (tab == TransactionsTab.ENTRIES) entriesListState.scrollToItem(0) else categoriesListState.scrollToItem(0)
    }
    // Switching sort mode resets only the Entries list.
    LaunchedEffect(sortMode) { entriesListState.scrollToItem(0) }

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
            FlowMenuToggle(flow = viewFilter, onPick = viewModel::pickViewFilter)
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
        // same chips, same independent scope, same Custom -> PeriodPickerSheet behavior, just reordered.
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                modifier = Modifier.weight(1f),
            )
            // Sort only applies to the Entries tab's own rendering — Categories has no concept of it.
            if (tab == TransactionsTab.ENTRIES) SortMenuButton(current = sortMode, onPick = viewModel::setSortMode)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                DateUtils.appliedRangeLabel(resolvedRange),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = colors.sub,
            )
            entries?.let {
                Text(
                    it.totalFormatted,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    color = colors.ink,
                )
            }
        }

        // The gap under the chips sits outside the scrolling list, so scrolled rows/cards clip a
        // clear band below them instead of running flush against them.
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 14.dp))
        // Only the visible tab is rendered, so only its rows are ever laid out. Until its first
        // state arrives nothing is drawn (never a flash of "No transactions…").
        when (tab) {
            TransactionsTab.ENTRIES -> {
                entries?.let {
                    EntriesList(
                        ui = it,
                        onRowClick = { id -> container.selectionState.editingTransactionId.value = id },
                        modifier = Modifier.fillMaxWidth(),
                        listState = entriesListState,
                    )
                }
            }
            TransactionsTab.CATEGORIES -> {
                rollups?.let {
                    CategoriesTab(
                        rollups = it.rollups,
                        onCategoryClick = { categoryId -> categoryEntriesSheetFor = categoryId },
                        modifier = Modifier.fillMaxWidth(),
                        listState = categoriesListState,
                    )
                }
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

    categoryEntriesSheetFor?.let { categoryId ->
        val categoryName = rollups?.rollups?.find { it.categoryId == categoryId }?.name ?: "Category"
        val categoryUi by remember(categoryId) { viewModel.entriesForCategory(categoryId) }.collectAsStateWithLifecycle()
        CategoryEntriesSheet(
            categoryName = categoryName,
            ui = categoryUi,
            onRowClick = { id ->
                categoryEntriesSheetFor = null
                container.selectionState.editingTransactionId.value = id
            },
            onDismiss = { categoryEntriesSheetFor = null },
        )
    }
}

/**
 * "on 21 Sep", "from 21 Sep to 27 Sep", or null before a custom range has been picked. No longer
 * called by this screen itself (round 10 replaced the EntryFilter-chip header sentence with the
 * period pill's own label) but kept — [EntryFilter]/[AnalyticsEngine.entryFilterRange] are both still
 * exercised directly by `CalendarBoundsTest`.
 */
internal fun headerDateClause(filter: EntryFilter, customRange: DateRange?, today: LocalDate): String? {
    fun clause(range: DateRange): String {
        fun label(d: LocalDate) = DateUtils.smartDate(d, today)
        return if (range.start == range.end) "on ${label(range.start)}" else "from ${label(range.start)} to ${label(range.end)}"
    }
    return AnalyticsEngine.entryFilterRange(filter, customRange, today)?.let(::clause)
}

/**
 * The sort icon pinned at the end of the date-filter chip row: a compact 2-item popup menu ("Sort by
 * date" / "Sort by entry"), styled to match [FlowMenuToggle]'s own popup pattern. [SyncSpendIcons.Swap]
 * is reused here as the closest existing icon to a dedicated sort glyph — no bespoke icon exists in
 * this app's hand-authored icon set, and adding one is out of scope for this change.
 */
@Composable
private fun SortMenuButton(current: EntriesSortMode, onPick: (EntriesSortMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val colors = SyncSpendTheme.colors
    Box {
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(34.dp)
                .background(colors.tile, RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { open = !open },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(SyncSpendIcons.Swap, contentDescription = "Sort", tint = colors.ink, modifier = Modifier.size(16.dp))
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
                        .width(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SortMenuItem("Sort by date", EntriesSortMode.BY_DATE, current) { onPick(it); open = false }
                    SortMenuItem("Sort by entry", EntriesSortMode.BY_ENTRY, current) { onPick(it); open = false }
                }
            }
        }
    }
}

@Composable
private fun SortMenuItem(label: String, value: EntriesSortMode, current: EntriesSortMode, onPick: (EntriesSortMode) -> Unit) {
    val colors = SyncSpendTheme.colors
    val selected = value == current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.selectedBrush else SolidColor(Color.Transparent), RoundedCornerShape(10.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPick(value) }
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) colors.onSelected else colors.ink)
    }
}
