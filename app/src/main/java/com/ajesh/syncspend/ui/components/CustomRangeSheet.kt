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
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate

/**
 * The Transactions "Custom" filter: just a From and a To date. Tapping either
 * tile opens the same calendar the subscription/reminder dates use (bounded to
 * the last couple of months — or back to the earliest entry — and never past
 * today), and three presets cover the usual look-backs. Picking a From after
 * To drags To along, and the other way round, so the range is never inverted.
 */
@Composable
fun CustomRangeSheet(
    initial: DateRange?,
    earliestTransactionDate: LocalDate?,
    onApply: (DateRange) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SyncSpendTheme.colors
    val today = remember { LocalDate.now() }
    var from by remember { mutableStateOf(initial?.start ?: today.withDayOfMonth(1)) }
    var to by remember { mutableStateOf(initial?.end ?: today) }
    var pickingFrom by remember { mutableStateOf(false) }
    var pickingTo by remember { mutableStateOf(false) }
    val presets = remember(today) { rangePresets(today) }
    val days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1

    DesignSheet(onDismiss = onDismiss) { close ->
        SheetHeader("Custom range", onClose = close)

        Row(
            modifier = Modifier.padding(top = 14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            DateTile("From", from, Modifier.weight(1f)) { pickingFrom = true }
            DateTile("To", to, Modifier.weight(1f)) { pickingTo = true }
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
            presets.forEach { (label, range) ->
                PickerChip(
                    label = label,
                    selected = from == range.start && to == range.end,
                    vertical = 8.dp,
                    radius = 12.dp,
                ) {
                    from = range.start
                    to = range.end
                }
            }
        }

        Row(modifier = Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SheetButton("Cancel", primary = false, modifier = Modifier.weight(1f), onClick = close)
            SheetButton("Apply", primary = true, modifier = Modifier.weight(1f)) {
                onApply(DateRange(from, to))
                close()
            }
        }

        if (pickingFrom) {
            DatePickerSheet(
                initial = from,
                title = "FROM",
                earliestTransactionDate = earliestTransactionDate,
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
                earliestTransactionDate = earliestTransactionDate,
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

/** The look-backs offered as one-tap presets, ending today. */
internal fun rangePresets(today: LocalDate): List<Pair<String, DateRange>> = listOf(
    "Last 60 days" to DateRange(today.minusDays(59), today),
    "Last 90 days" to DateRange(today.minusDays(89), today),
    "Last 6 months" to DateRange(today.minusMonths(6).plusDays(1), today),
)

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
