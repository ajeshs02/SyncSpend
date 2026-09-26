package com.ajesh.syncspend.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.domain.model.RangeSelection
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * From/To calendar for the Transactions "Custom" filter, "hotel booking" style:
 * one continuous scrolling list of months (last ~3 months through the current
 * month, further back only if an entry is older), start/end circles joined by a
 * soft band. Tap a start day then an end day — or tap a month's title to take
 * the whole month, and another title to take every month in between (see
 * [RangeSelection] for the exact rules).
 */
@Composable
fun DateRangePickerSheet(
    initial: DateRange?,
    earliestTransactionDate: LocalDate?,
    onApply: (DateRange) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SyncSpendTheme.colors
    val today = remember { LocalDate.now() }
    val upper = remember { YearMonth.from(today) }
    val lower = remember(earliestTransactionDate, initial) {
        listOfNotNull(
            upper.minusMonths(3),
            earliestTransactionDate?.let { YearMonth.from(it) },
            initial?.let { YearMonth.from(it.start) },
        ).minOf { it }
    }
    val months = remember(lower) { generateSequence(lower) { it.plusMonths(1) }.takeWhile { !it.isAfter(upper) }.toList() }

    var selection by remember { mutableStateOf(initial?.let { RangeSelection(it.start, it.end) } ?: RangeSelection()) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = months.lastIndex.coerceAtLeast(0))
    val scope = rememberCoroutineScope()
    val listHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.38f).coerceIn(240.dp, 360.dp)
    val presets = remember(today) { rangePresets(today) }

    DesignSheet(onDismiss = onDismiss) { close ->
        SheetHeader("Custom range", onClose = close)

        Row(
            modifier = Modifier.padding(top = 14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RangeTile("From", selection.start, active = selection.start == null || selection.isComplete, modifier = Modifier.weight(1f))
            RangeTile("To", selection.end, active = selection.start != null && selection.end == null, modifier = Modifier.weight(1f))
        }
        Text(
            selection.range?.let { r ->
                if (selection.end == null) "Now tap an end date" else "${r.dayCount} ${if (r.dayCount == 1L) "day" else "days"} selected"
            } ?: "Tap a start date, then an end date — or tap a month name",
            fontSize = 10.5.sp,
            color = colors.sub,
            modifier = Modifier.padding(top = 8.dp),
        )

        Row(
            modifier = Modifier.padding(top = 10.dp).fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            presets.forEach { (label, range) ->
                PickerChip(
                    label = label,
                    selected = selection.isComplete && selection.range == range,
                    vertical = 7.dp,
                    radius = 12.dp,
                ) {
                    selection = RangeSelection(range.start, range.end)
                    val target = months.indexOf(YearMonth.from(range.start)).coerceAtLeast(0)
                    scope.launch { listState.animateScrollToItem(target) }
                }
            }
        }

        Row(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.sub,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 3.dp),
                )
            }
        }

        LazyColumn(state = listState, modifier = Modifier.height(listHeight)) {
            items(months, key = { it.toString() }, contentType = { "month" }) { month ->
                MonthBlock(
                    month = month,
                    selection = selection,
                    today = today,
                    onDay = { selection = selection.tapDay(it) },
                    onMonth = { selection = selection.tapMonth(month, today) },
                )
            }
        }

        Row(modifier = Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SheetButton("Cancel", primary = false, modifier = Modifier.weight(1f), onClick = close)
            SheetButton("Apply", primary = true, modifier = Modifier.weight(1f), enabled = selection.start != null) {
                selection.range?.let(onApply)
                close()
            }
        }
    }
}

private fun rangePresets(today: LocalDate): List<Pair<String, DateRange>> = listOf(
    "This month" to DateRange(today.withDayOfMonth(1), today),
    "Last 30 days" to DateRange(today.minusDays(29), today),
    "Last 3 months" to DateRange(today.minusMonths(3).plusDays(1), today),
    "This year" to DateRange(today.withDayOfYear(1), today),
)

@Composable
private fun RangeTile(label: String, date: LocalDate?, active: Boolean, modifier: Modifier) {
    val colors = SyncSpendTheme.colors
    val border by animateColorAsState(if (active) colors.acc else colors.line, tween(180), label = "range-tile")
    Column(
        modifier = modifier
            .background(colors.tile, RoundedCornerShape(14.dp))
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.sub)
        Text(
            date?.let(DateUtils::shortDateYear) ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun MonthBlock(
    month: YearMonth,
    selection: RangeSelection,
    today: LocalDate,
    onDay: (LocalDate) -> Unit,
    onMonth: () -> Unit,
) {
    val colors = SyncSpendTheme.colors
    val offset = month.atDay(1).dayOfWeek.value % 7 // Sunday-first, like the single-date calendar
    val length = month.lengthOfMonth()
    val weeks = (offset + length + 6) / 7
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onMonth)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${month.month.getDisplayName(TextStyle.FULL, Locale.US)} ${month.year}",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Text("Whole month", fontSize = 10.sp, color = colors.acc)
        }
        repeat(weeks) { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val day = week * 7 + col - offset + 1
                    RangeDayCell(
                        date = if (day in 1..length) month.atDay(day) else null,
                        column = col,
                        selection = selection,
                        today = today,
                        modifier = Modifier.weight(1f),
                        onClick = onDay,
                    )
                }
            }
        }
    }
}

@Composable
private fun RangeDayCell(
    date: LocalDate?,
    column: Int,
    selection: RangeSelection,
    today: LocalDate,
    modifier: Modifier,
    onClick: (LocalDate) -> Unit,
) {
    if (date == null) {
        Box(modifier.height(40.dp))
        return
    }
    val colors = SyncSpendTheme.colors
    val start = selection.start
    val end = selection.end ?: start
    val isStart = date == start
    val isEnd = date == end
    val endpoint = isStart || isEnd
    val inSpan = start != null && end != null && start != end && !date.isBefore(start) && !date.isAfter(end)
    val future = date.isAfter(today)

    Box(
        modifier = modifier
            .height(40.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !future) { onClick(date) },
        contentAlignment = Alignment.Center,
    ) {
        if (inSpan) {
            val capStart = isStart || column == 0 || date.dayOfMonth == 1
            val capEnd = isEnd || column == 6 || date.dayOfMonth == date.lengthOfMonth()
            val cap = 17.dp
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(
                        colors.acc.copy(alpha = 0.16f),
                        RoundedCornerShape(
                            topStart = if (capStart) cap else 0.dp,
                            bottomStart = if (capStart) cap else 0.dp,
                            topEnd = if (capEnd) cap else 0.dp,
                            bottomEnd = if (capEnd) cap else 0.dp,
                        ),
                    ),
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(if (endpoint) colors.selectedBrush else SolidColor(Color.Transparent), CircleShape)
                .then(if (date == today && !endpoint) Modifier.border(1.dp, colors.acc, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = when {
                    endpoint -> colors.onSelected
                    future -> colors.sub.copy(alpha = 0.35f)
                    else -> colors.ink
                },
            )
        }
    }
}
