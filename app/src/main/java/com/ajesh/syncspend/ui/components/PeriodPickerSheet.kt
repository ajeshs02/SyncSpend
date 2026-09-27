package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.domain.model.CalendarBounds
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * The design's month/year scope picker, plus rolling "Last 3 / 6 months"
 * windows. [showAllTime] adds the "All Time" option — Home leaves it out, the
 * CSV export picker needs it. The browsable range runs from the oldest entry's
 * month to this month (no buffer), so months and windows the entries don't
 * reach yet are greyed out.
 */
@Composable
fun PeriodPickerSheet(
    currentScope: ScopePeriod,
    earliestTransactionDate: LocalDate?,
    showAllTime: Boolean,
    onApply: (ScopePeriod) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val upperBound = remember { YearMonth.from(today) }
    val lowerBound = remember(earliestTransactionDate) { CalendarBounds.dataLowerMonth(upperBound, earliestTransactionDate) }

    var draft by remember { mutableStateOf(currentScope) }
    var pickerYear by remember {
        mutableIntStateOf(
            when (currentScope) {
                is ScopePeriod.Month -> currentScope.yearMonth.year
                is ScopePeriod.Year -> currentScope.year
                is ScopePeriod.LastMonths -> currentScope.endMonth.year
                ScopePeriod.AllTime -> upperBound.year
            }.coerceIn(lowerBound.year, upperBound.year),
        )
    }

    DesignSheet(onDismiss = onDismiss) { close ->
        PeriodPickerBody(
            draft = draft,
            onDraftChange = { draft = it },
            pickerYear = pickerYear,
            onYearChange = { pickerYear = it },
            lowerBound = lowerBound,
            upperBound = upperBound,
            showAllTime = showAllTime,
            onCancel = close,
            onApply = {
                onApply(draft)
                close()
            },
        )
    }
}

/** The sheet's content, state-hoisted so it can be rendered on its own (screenshot tests). */
@Composable
internal fun PeriodPickerBody(
    draft: ScopePeriod,
    onDraftChange: (ScopePeriod) -> Unit,
    pickerYear: Int,
    onYearChange: (Int) -> Unit,
    lowerBound: YearMonth,
    upperBound: YearMonth,
    showAllTime: Boolean,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SheetHeader("Select period", onClose = onCancel)

        Row(modifier = Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            SquareIconButton(
                SyncSpendIcons.Prev, { onYearChange(pickerYear - 1) },
                size = 32.dp, radius = 11.dp, iconSize = 17.dp, enabled = pickerYear > lowerBound.year,
            )
            Text(
                pickerYear.toString(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = SyncSpendTheme.colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            SquareIconButton(
                SyncSpendIcons.Next, { onYearChange(pickerYear + 1) },
                size = 32.dp, radius = 11.dp, iconSize = 17.dp, enabled = pickerYear < upperBound.year,
            )
        }

        // Three rolling/whole-year shortcuts in one row (same 3-column rhythm as the month grid below).
        Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(3, 6).forEach { n ->
                    PickerChip(
                        label = "Last $n months",
                        selected = (draft as? ScopePeriod.LastMonths)?.months == n,
                        enabled = CalendarBounds.windowEnabled(n, upperBound, lowerBound),
                        modifier = Modifier.weight(1f),
                        vertical = 10.dp,
                        radius = 12.dp,
                        singleLine = true,
                    ) { onDraftChange(ScopePeriod.LastMonths(n, upperBound)) }
                }
                PickerChip(
                    label = "Year $pickerYear",
                    selected = draft is ScopePeriod.Year && draft.year == pickerYear,
                    modifier = Modifier.weight(1f),
                    vertical = 10.dp,
                    radius = 12.dp,
                    singleLine = true,
                ) { onDraftChange(ScopePeriod.Year(pickerYear)) }
            }
            if (showAllTime) {
                PickerChip(
                    label = "All Time",
                    selected = draft is ScopePeriod.AllTime,
                    modifier = Modifier.fillMaxWidth(),
                    vertical = 10.dp,
                    radius = 12.dp,
                ) { onDraftChange(ScopePeriod.AllTime) }
            }
        }

        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Month.values().toList().chunked(3).forEach { rowMonths ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowMonths.forEach { month ->
                        val ym = YearMonth.of(pickerYear, month)
                        val inRange = !ym.isBefore(lowerBound) && !ym.isAfter(upperBound)
                        PickerChip(
                            label = month.getDisplayName(TextStyle.SHORT, Locale.US),
                            selected = draft is ScopePeriod.Month && draft.yearMonth == ym,
                            enabled = inRange,
                            modifier = Modifier.weight(1f),
                            vertical = 12.dp,
                            radius = 14.dp,
                        ) { onDraftChange(ScopePeriod.Month(ym)) }
                    }
                }
            }
        }

        Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SheetButton("Cancel", primary = false, modifier = Modifier.weight(1f), onClick = onCancel)
            SheetButton("Apply", primary = true, modifier = Modifier.weight(1f), onClick = onApply)
        }
    }
}

/** Selectable pill used for scope chips and month cells. */
@Composable
fun PickerChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    vertical: androidx.compose.ui.unit.Dp = 10.dp,
    radius: androidx.compose.ui.unit.Dp = 12.dp,
    /** Keep the label on one line (rolling-window chips, where three share a row). */
    singleLine: Boolean = false,
    /** Space between the label and the chip's left/right edge (chips in a scrolling row, where width is free). */
    horizontal: androidx.compose.ui.unit.Dp = 0.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                if (selected) SyncSpendTheme.colors.selectedBrush else SolidColor(SyncSpendTheme.colors.pill),
                RoundedCornerShape(radius),
            )
            .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(radius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = horizontal, vertical = vertical),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            // Three of these share a row, so they run a touch smaller to keep breathing room on narrow phones.
            style = if (singleLine) MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp) else MaterialTheme.typography.labelLarge,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            softWrap = !singleLine,
            color = when {
                selected -> SyncSpendTheme.colors.onSelected
                !enabled -> SyncSpendTheme.colors.sub.copy(alpha = 0.35f)
                else -> SyncSpendTheme.colors.sub
            },
        )
    }
}

/** Cancel / Apply style footer button (46dp, 15dp radius). */
@Composable
fun SheetButton(label: String, primary: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(46.dp)
            .background(
                if (primary) SyncSpendTheme.colors.button.copy(alpha = if (enabled) 1f else 0.35f) else SyncSpendTheme.colors.card,
                RoundedCornerShape(15.dp),
            )
            .then(if (primary) Modifier else Modifier.border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(15.dp)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (primary) SyncSpendTheme.colors.onButton else SyncSpendTheme.colors.ink,
        )
    }
}
