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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * The design's month/year/all-time scope picker, with one deliberate change
 * from the design (user requirement): rather than letting the year nav page
 * back indefinitely, the browsable range defaults to the last ~3 months
 * through the current month, extended further back only if an existing
 * transaction is older than that — no need to page through years of empty
 * months to reach "today."
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PeriodPickerSheet(
    currentScope: ScopePeriod,
    earliestTransactionDate: LocalDate?,
    onApply: (ScopePeriod) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
) {
    val today = remember { LocalDate.now() }
    val upperBound = remember { YearMonth.from(today) }
    val lowerBound = remember(earliestTransactionDate) {
        val defaultLower = upperBound.minusMonths(3)
        val earliestMonth = earliestTransactionDate?.let { YearMonth.from(it) }
        if (earliestMonth != null && earliestMonth.isBefore(defaultLower)) earliestMonth else defaultLower
    }

    var draft by remember { mutableStateOf(currentScope) }
    var pickerYear by remember {
        mutableStateOf(
            when (currentScope) {
                is ScopePeriod.Month -> currentScope.yearMonth.year
                is ScopePeriod.Year -> currentScope.year
                ScopePeriod.AllTime -> upperBound.year
            },
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = SyncSpendTheme.colors.sheet) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Select period", style = MaterialTheme.typography.titleMedium, color = SyncSpendTheme.colors.ink)
                CloseChip(onClick = onDismiss)
            }

            androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(
                    icon = SyncSpendIcons.Prev,
                    enabled = pickerYear > lowerBound.year,
                    onClick = { pickerYear-- },
                )
                Text(
                    pickerYear.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = SyncSpendTheme.colors.ink,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                RoundIconButton(
                    icon = SyncSpendIcons.Next,
                    enabled = pickerYear < upperBound.year,
                    onClick = { pickerYear++ },
                )
            }

            androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ScopeChip(
                    label = "Year $pickerYear",
                    selected = draft is ScopePeriod.Year && (draft as ScopePeriod.Year).year == pickerYear,
                    modifier = Modifier.weight(1f),
                    onClick = { draft = ScopePeriod.Year(pickerYear) },
                )
                ScopeChip(
                    label = "All Time",
                    selected = draft is ScopePeriod.AllTime,
                    modifier = Modifier.weight(1f),
                    onClick = { draft = ScopePeriod.AllTime },
                )
            }

            androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(180.dp),
            ) {
                items(Month.values().toList()) { month ->
                    val ym = YearMonth.of(pickerYear, month)
                    val inRange = !ym.isBefore(lowerBound) && !ym.isAfter(upperBound)
                    val selected = draft is ScopePeriod.Month && (draft as ScopePeriod.Month).yearMonth == ym
                    MonthCell(
                        label = month.getDisplayName(TextStyle.SHORT, Locale.US),
                        selected = selected,
                        enabled = inRange,
                        onClick = { draft = ScopePeriod.Month(ym) },
                    )
                }
            }

            androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(15.dp))
                        .background(SyncSpendTheme.colors.card, RoundedCornerShape(15.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge, color = SyncSpendTheme.colors.ink)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .background(SyncSpendTheme.colors.dark, RoundedCornerShape(15.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            onApply(draft)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Apply", style = MaterialTheme.typography.labelLarge, color = androidx.compose.ui.graphics.Color.White)
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CloseChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(SyncSpendTheme.colors.tile, RoundedCornerShape(10.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(SyncSpendIcons.Close, null, tint = SyncSpendTheme.colors.ink, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun RoundIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(SyncSpendTheme.colors.tile, RoundedCornerShape(11.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            null,
            tint = if (enabled) SyncSpendTheme.colors.ink else SyncSpendTheme.colors.sub.copy(alpha = 0.4f),
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun ScopeChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(
                if (selected) SyncSpendTheme.colors.darkGradient else androidx.compose.ui.graphics.SolidColor(SyncSpendTheme.colors.pill),
                RoundedCornerShape(12.dp),
            )
            .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) androidx.compose.ui.graphics.Color.White else SyncSpendTheme.colors.sub,
        )
    }
}

@Composable
private fun MonthCell(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(
                if (selected) SyncSpendTheme.colors.darkGradient else androidx.compose.ui.graphics.SolidColor(SyncSpendTheme.colors.pill),
                RoundedCornerShape(14.dp),
            )
            .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                selected -> androidx.compose.ui.graphics.Color.White
                !enabled -> SyncSpendTheme.colors.sub.copy(alpha = 0.35f)
                else -> SyncSpendTheme.colors.sub
            },
        )
    }
}
