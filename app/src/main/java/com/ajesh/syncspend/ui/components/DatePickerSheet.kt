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
 * The year- and month-level Prev/Next enablement for [DatePickerSheet]'s two navigation rows, pulled
 * out as plain functions so the round-9 bug (year arrows following month-granularity availability) is
 * unit-testable without Compose/Robolectric. The two granularities are intentionally independent: a
 * year arrow cares only whether a different *year* is reachable, a month arrow only whether a
 * different *month* within the current browsable span is.
 */
internal object DatePickerNav {
    fun yearPrevEnabled(viewing: YearMonth, lower: YearMonth): Boolean = viewing.year > lower.year
    fun yearNextEnabled(viewing: YearMonth, upper: YearMonth): Boolean = viewing.year < upper.year
    fun monthPrevEnabled(viewing: YearMonth, lower: YearMonth): Boolean = viewing.isAfter(lower)
    fun monthNextEnabled(viewing: YearMonth, upper: YearMonth): Boolean = viewing.isBefore(upper)
}

/**
 * The design's custom calendar sheet, used for every date pick in the app
 * (Add Entry, Edit Entry, custom ranges, subscription / reminder dates).
 *
 * The caller decides how far back the months go with [minMonth] (see
 * [com.ajesh.syncspend.domain.model.CalendarBounds]; the month of [initial] is always reachable) and, in
 * the other direction, [maxMonth] for future dates such as a subscription's next due date. [minDate] and
 * [maxDate] grey out the days outside them inside the browsable months. The grid is always six rows tall
 * so the sheet never changes height as you page between months.
 */
@Composable
fun DatePickerSheet(
    initial: LocalDate,
    onApply: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    title: String = "ENTRY DATE",
    /** Earliest month that can be browsed; null = this month. */
    minMonth: YearMonth? = null,
    maxMonth: YearMonth? = null,
    /** Days before this are shown but can't be picked (e.g. the oldest entry, for a from/to range). */
    minDate: LocalDate? = null,
    /** Days after this are shown but can't be picked (e.g. today, for a from/to range). */
    maxDate: LocalDate? = null,
) {
    val initialMonth = remember { YearMonth.from(initial) }
    val now = remember { YearMonth.now() }
    val lower = remember(minMonth) { minOf(minMonth ?: now, initialMonth) }
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

        // Year-level jump above the month-level one, same two-tier navigation Home's own period
        // picker uses (PeriodPickerSheet) — otherwise reaching a date years back meant tapping
        // "previous month" dozens of times.
        Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            SquareIconButton(
                SyncSpendIcons.Prev, { viewing = maxOf(viewing.minusMonths(12), lower) },
                // Year-granularity check — independent of the month row below. Comparing full
                // YearMonths here (as the month row correctly does) was the round-9 bug: it disabled
                // this button only once the *month* ran out, not the *year*, so e.g. viewing=Jul 2026
                // with lower=Mar 2026 left this enabled even though there's no earlier year to jump to.
                size = 32.dp, radius = 11.dp, iconSize = 17.dp, enabled = DatePickerNav.yearPrevEnabled(viewing, lower),
            )
            Text(
                viewing.year.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            SquareIconButton(
                SyncSpendIcons.Next, { viewing = minOf(viewing.plusMonths(12), upper) },
                size = 32.dp, radius = 11.dp, iconSize = 17.dp, enabled = DatePickerNav.yearNextEnabled(viewing, upper),
            )
        }

        Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            SquareIconButton(
                SyncSpendIcons.Prev, { viewing = viewing.minusMonths(1) },
                size = 32.dp, radius = 11.dp, iconSize = 17.dp, enabled = DatePickerNav.monthPrevEnabled(viewing, lower),
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
                size = 32.dp, radius = 11.dp, iconSize = 17.dp, enabled = DatePickerNav.monthNextEnabled(viewing, upper),
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
            MonthGrid(month = month, selected = selected, today = today, minDate = minDate, maxDate = maxDate, onPick = { selected = it })
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
private fun MonthGrid(month: YearMonth, selected: LocalDate, today: LocalDate, minDate: LocalDate?, maxDate: LocalDate?, onPick: (LocalDate) -> Unit) {
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
                            enabled = (minDate == null || !date.isBefore(minDate)) && (maxDate == null || !date.isAfter(maxDate)),
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
