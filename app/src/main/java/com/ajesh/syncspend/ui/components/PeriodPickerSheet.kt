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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * The design's month/year scope picker, plus rolling "Last 3 / 6 / 12 months"
 * windows. [showAllTime] adds the "All Time" option — Home leaves it out, the
 * CSV export picker needs it. Rather than letting the year nav page back
 * indefinitely, the browsable range defaults to the last 2 months through the
 * current month, extended further back only if an existing transaction is
 * older than that.
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
    val lowerBound = remember(earliestTransactionDate) {
        val defaultLower = upperBound.minusMonths(2)
        val earliestMonth = earliestTransactionDate?.let { YearMonth.from(it) }
        if (earliestMonth != null && earliestMonth.isBefore(defaultLower)) earliestMonth else defaultLower
    }

    var draft by remember { mutableStateOf(currentScope) }
    var pickerYear by remember {
        mutableStateOf(
            when (currentScope) {
                is ScopePeriod.Month -> currentScope.yearMonth.year
                is ScopePeriod.Year -> currentScope.year
                is ScopePeriod.LastMonths -> currentScope.endMonth.year
                ScopePeriod.AllTime -> upperBound.year
            }.coerceIn(lowerBound.year, upperBound.year),
        )
    }

    DesignSheet(onDismiss = onDismiss) { close ->
        SheetHeader("Select period", onClose = close)

        Row(modifier = Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            SquareIconButton(
                SyncSpendIcons.Prev, { pickerYear-- },
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
                SyncSpendIcons.Next, { pickerYear++ },
                size = 32.dp, radius = 11.dp, iconSize = 17.dp, enabled = pickerYear < upperBound.year,
            )
        }

        val options = buildList {
            add(Triple("Year $pickerYear", draft is ScopePeriod.Year && (draft as ScopePeriod.Year).year == pickerYear) { draft = ScopePeriod.Year(pickerYear) })
            listOf(3, 6, 12).forEach { n ->
                add(Triple("Last $n months", (draft as? ScopePeriod.LastMonths)?.months == n) { draft = ScopePeriod.LastMonths(n, upperBound) })
            }
            if (showAllTime) add(Triple("All Time", draft is ScopePeriod.AllTime) { draft = ScopePeriod.AllTime })
        }
        Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            options.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    pair.forEach { (label, selected, onPick) ->
                        PickerChip(
                            label = label,
                            selected = selected,
                            modifier = Modifier.weight(1f),
                            vertical = 10.dp,
                            radius = 12.dp,
                            onClick = onPick,
                        )
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
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
                            selected = draft is ScopePeriod.Month && (draft as ScopePeriod.Month).yearMonth == ym,
                            enabled = inRange,
                            modifier = Modifier.weight(1f),
                            vertical = 12.dp,
                            radius = 14.dp,
                        ) { draft = ScopePeriod.Month(ym) }
                    }
                }
            }
        }

        Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SheetButton("Cancel", primary = false, modifier = Modifier.weight(1f), onClick = close)
            SheetButton("Apply", primary = true, modifier = Modifier.weight(1f)) {
                onApply(draft)
                close()
            }
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
            .padding(vertical = vertical),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
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
