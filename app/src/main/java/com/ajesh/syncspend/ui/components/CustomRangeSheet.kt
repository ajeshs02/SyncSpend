package com.ajesh.syncspend.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.domain.model.CalendarBounds
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

/**
 * The Transactions "Custom" filter: just a From and a To date. Tapping either tile opens the same
 * calendar the other date picks use, bounded to the days the entries actually cover (oldest entry to
 * today, no buffer), and three presets cover the usual look-backs: the first of the month N-1 months
 * back up to today. A preset is greyed out until the entries reach back that far. Picking a From after
 * To drags To along, and the other way round, so the range is never inverted.
 */
@Composable
fun CustomRangeSheet(
    initial: DateRange?,
    earliestTransactionDate: LocalDate?,
    onApply: (DateRange) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val now = remember { YearMonth.from(today) }
    val lowerMonth = remember(earliestTransactionDate) { CalendarBounds.dataLowerMonth(now, earliestTransactionDate) }
    // A fresh range starts this month, but never before the oldest entry.
    var from by remember { mutableStateOf(initial?.start ?: today.withDayOfMonth(1).let { start -> earliestTransactionDate?.takeIf { it.isAfter(start) && !it.isAfter(today) } ?: start }) }
    var to by remember { mutableStateOf(initial?.end ?: today) }
    var pickingFrom by remember { mutableStateOf(false) }
    var pickingTo by remember { mutableStateOf(false) }
    val presets = remember(today, lowerMonth) { rangePresets(today, lowerMonth) }

    DesignSheet(onDismiss = onDismiss) { close ->
        CustomRangeBody(
            from = from,
            to = to,
            presets = presets,
            onPickFrom = { pickingFrom = true },
            onPickTo = { pickingTo = true },
            onPreset = { range ->
                from = range.start
                to = range.end
            },
            onCancel = close,
            onApply = {
                onApply(DateRange(from, to))
                close()
            },
        )

        if (pickingFrom) {
            DatePickerSheet(
                initial = from,
                title = "FROM",
                minMonth = lowerMonth,
                minDate = earliestTransactionDate,
                maxDate = today,
                onApply = { picked ->
                    from = picked
                    if (to.isBefore(picked)) to = picked
                },
                onDismiss = { pickingFrom = false },
            )
        }
        if (pickingTo) {
            DatePickerSheet(
                initial = to,
                title = "TO",
                minMonth = lowerMonth,
                minDate = earliestTransactionDate,
                maxDate = today,
                onApply = { picked ->
                    to = picked
                    if (from.isAfter(picked)) from = picked
                },
                onDismiss = { pickingTo = false },
            )
        }
    }
}

/** The sheet's content, state-hoisted so it can be rendered on its own (screenshot tests). */
@Composable
internal fun CustomRangeBody(
    from: LocalDate,
    to: LocalDate,
    presets: List<RangePreset>,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    onPreset: (DateRange) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SyncSpendTheme.colors
    val days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1
    Column(modifier = modifier) {
        SheetHeader("Custom range", onClose = onCancel)

        Row(
            modifier = Modifier.padding(top = 14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            DateTile("From", from, Modifier.weight(1f), onPickFrom)
            DateTile("To", to, Modifier.weight(1f), onPickTo)
        }
        Text(
            "$days ${if (days == 1L) "day" else "days"} selected",
            fontSize = 10.5.sp,
            color = colors.sub,
            modifier = Modifier.padding(top = 8.dp),
        )

        Row(
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            presets.forEach { preset ->
                PickerChip(
                    label = preset.label,
                    selected = from == preset.range.start && to == preset.range.end,
                    enabled = preset.enabled,
                    vertical = 8.dp,
                    horizontal = 14.dp,
                    radius = 12.dp,
                ) { onPreset(preset.range) }
            }
        }

        Row(modifier = Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SheetButton("Cancel", primary = false, modifier = Modifier.weight(1f), onClick = onCancel)
            SheetButton("Apply", primary = true, modifier = Modifier.weight(1f), onClick = onApply)
        }
    }
}

/** A one-tap look-back; [enabled] is false while the entries don't reach back that far. */
internal data class RangePreset(val label: String, val range: DateRange, val enabled: Boolean)

/**
 * "Last 2 / 3 / 6 months": from the first of the month N-1 back through today (Sep 27: Last 3 months = Jul 1
 * to Sep 27). [lowerBound] is the oldest month worth browsing (see [CalendarBounds.dataLowerMonth]).
 */
internal fun rangePresets(today: LocalDate, lowerBound: YearMonth): List<RangePreset> {
    val now = YearMonth.from(today)
    return listOf(2, 3, 6).map { months ->
        RangePreset(
            label = "Last $months months",
            range = DateRange(now.minusMonths(months - 1L).atDay(1), today),
            enabled = CalendarBounds.windowEnabled(months, now, lowerBound),
        )
    }
}

@Composable
private fun DateTile(label: String, date: LocalDate, modifier: Modifier, onClick: () -> Unit) {
    val colors = SyncSpendTheme.colors
    Row(
        modifier = modifier
            .background(colors.tile, RoundedCornerShape(14.dp))
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.sub)
            Text(
                DateUtils.shortDateYear(date),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(SyncSpendIcons.Cal, null, tint = colors.sub, modifier = Modifier.size(16.dp))
    }
}
