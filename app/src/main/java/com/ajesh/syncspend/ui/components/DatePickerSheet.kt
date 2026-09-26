package com.ajesh.syncspend.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * The design's custom calendar sheet, used for every date pick in the app
 * (Add Entry, Edit Entry, subscription / reminder dates).
 *
 * Browsable months default to the last 2 months through the current month —
 * extended further back only if an existing entry is older — so there is no
 * endless scrollback through empty years. Callers picking future dates (a
 * subscription's next due date) pass [minMonth]/[maxMonth]; the month of
 * [initial] is always reachable. The grid is always six rows tall so the sheet
 * never changes height as you page between months.
 */
@Composable
fun DatePickerSheet(
    initial: LocalDate,
    onApply: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    earliestTransactionDate: LocalDate? = null,
    title: String = "ENTRY DATE",
    minMonth: YearMonth? = null,
    maxMonth: YearMonth? = null,
    /** Days after this are shown but can't be picked (e.g. today, for a from/to range). */
    maxDate: LocalDate? = null,
) {
    val initialMonth = remember { YearMonth.from(initial) }
    val now = remember { YearMonth.now() }
    val lower = remember(earliestTransactionDate, minMonth) {
        val base = minMonth ?: minOf(now.minusMonths(2), earliestTransactionDate?.let { YearMonth.from(it) } ?: now)
        minOf(base, initialMonth)
    }
    val upper = remember(maxMonth) { maxOf(maxMonth ?: now, initialMonth) }

    var selected by remember { mutableStateOf(initial) }
    var viewing by remember { mutableStateOf(initialMonth) }
    val colors = SyncSpendTheme.colors
    val today = remember { LocalDate.now() }

    DesignSheet(onDismiss = onDismiss) { close ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(title, fontSize = 10.5.sp, letterSpacing = 0.63.sp, color = colors.sub)
                Text(
                    DateUtils.longDate(selected),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                    color = colors.ink,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            SquareIconButton(SyncSpendIcons.Close, close, size = 30.dp)
        }

        Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            SquareIconButton(
                SyncSpendIcons.Prev, { viewing = viewing.minusMonths(1) },
                size = 32.dp, radius = 11.dp, iconSize = 17.dp, enabled = viewing.isAfter(lower),
            )
            Text(
                "${viewing.month.getDisplayName(TextStyle.FULL, Locale.US)} ${viewing.year}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            SquareIconButton(
                SyncSpendIcons.Next, { viewing = viewing.plusMonths(1) },
                size = 32.dp, radius = 11.dp, iconSize = 17.dp, enabled = viewing.isBefore(upper),
            )
        }

        Row(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.sub,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                )
            }
        }

        AnimatedContent(
            targetState = viewing,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(220)) { it / 5 * dir } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(160)) { -it / 5 * dir } + fadeOut(tween(120)))
            },
            modifier = Modifier.padding(top = 4.dp),
            label = "calendar-month",
        ) { month ->
            MonthGrid(month = month, selected = selected, today = today, maxDate = maxDate, onPick = { selected = it })
        }

        Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SheetButton("Cancel", primary = false, modifier = Modifier.weight(1f), onClick = close)
            SheetButton("Apply", primary = true, modifier = Modifier.weight(1f)) {
                onApply(selected)
                close()
            }
        }
    }
}

/** Always 6 week-rows (42 cells) so every month occupies the same height. */
@Composable
private fun MonthGrid(month: YearMonth, selected: LocalDate, today: LocalDate, maxDate: LocalDate?, onPick: (LocalDate) -> Unit) {
    val firstOffset = month.atDay(1).dayOfWeek.value % 7 // Sunday-first, like the design
    val length = month.lengthOfMonth()
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(6) { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(7) { col ->
                    val day = week * 7 + col - firstOffset + 1
                    if (day in 1..length) {
                        val date = month.atDay(day)
                        DayCell(
                            day = day,
                            selected = date == selected,
                            isToday = date == today,
                            enabled = maxDate == null || !date.isAfter(maxDate),
                            modifier = Modifier.weight(1f),
                            onClick = { onPick(date) },
                        )
                    } else {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, selected: Boolean, isToday: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = SyncSpendTheme.colors
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(if (selected) colors.selectedBrush else SolidColor(Color.Transparent), CircleShape)
            .then(if (isToday && !selected) Modifier.border(1.dp, colors.acc, CircleShape) else Modifier)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.toString(),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            color = when {
                selected -> colors.onSelected
                !enabled -> colors.sub.copy(alpha = 0.35f)
                else -> colors.ink
            },
        )
    }
}
