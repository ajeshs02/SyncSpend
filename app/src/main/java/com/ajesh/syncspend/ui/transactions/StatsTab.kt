package com.ajesh.syncspend.ui.transactions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.domain.model.StatsRange
import com.ajesh.syncspend.domain.model.TipTone
import com.ajesh.syncspend.ui.components.ChipsRow
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/**
 * Stats tab body: top-category card, 2x2 tiles, six-month bars, where the money goes, weekday
 * pattern, tips & suggestions, findings and the period-over-period card. The range chips above it
 * live in [TransactionsScreen] (so they are on screen before the numbers are computed).
 *
 * A lazy list: only the cards on screen are composed, so opening Stats (or arriving from Home's
 * hero card while the tab pill is still sliding) builds a handful of cards, not all of them; a
 * chart's grow-in starts as its card scrolls into view.
 */
@Composable
fun StatsTab(stats: StatsUi, modifier: Modifier = Modifier) {
    val colors = SyncSpendTheme.colors
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 2.dp, bottom = SyncSpendChrome.screenBottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item(key = "top", contentType = "top") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.mintGradient, RoundedCornerShape(22.dp))
                    .padding(17.dp),
            ) {
                // This card's background is colors.mintGradient, the flat accent (same as chartFill,
                // same as everywhere else it's used) — colors.mink is the established "text on the
                // accent fill" token, not colors.pos (tuned for plain card/sheet backgrounds).
                Text(stats.kicker, fontSize = 11.sp, color = colors.mink.copy(alpha = 0.7f))
                Row(
                    modifier = Modifier.padding(top = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(SyncSpendIcons.iconFor(stats.topIconKey), null, tint = colors.mink, modifier = Modifier.size(18.dp)) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stats.topName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.mink, maxLines = 1)
                        Text(stats.topShareLine, fontSize = 11.sp, color = colors.mink.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp))
                    }
                    Text(stats.topTotal, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.19).sp, color = colors.mink)
                }
            }
        }

        itemsIndexed(stats.tiles.chunked(2), key = { index, _ -> "tiles-$index" }, contentType = { _, _ -> "tiles" }) { _, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { tile ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(colors.card, RoundedCornerShape(18.dp))
                            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                            .padding(14.dp),
                    ) {
                        Text(tile.label, fontSize = 10.5.sp, lineHeight = 13.sp, color = colors.sub, maxLines = 2)
                        // A good figure is dark text on the same mint gradient as the Top spending card
                        // (plain green text never matched the theme); a bad one stays red.
                        Text(
                            tile.value,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.19).sp,
                            color = when (tile.tone) {
                                StatTone.NEUTRAL -> colors.ink
                                StatTone.POSITIVE -> colors.mink
                                StatTone.NEGATIVE -> colors.neg
                            },
                            // Every value carries the chip's vertical padding, so tiles stay level whether or not one has a chip.
                            modifier = Modifier.padding(top = 5.dp).then(
                                if (tile.tone == StatTone.POSITIVE) Modifier.mintChip() else Modifier.padding(vertical = 2.dp),
                            ),
                        )
                        Text(tile.note, fontSize = 10.sp, color = colors.sub, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }

        stats.pace?.let { pace ->
            item(key = "pace", contentType = "card") { StatsCard(title = "Month pace") { PaceBar(pace) } }
        }

        item(key = "monthly", contentType = "card") {
            StatsCard(title = "Last 6 months") {
                BarChart(stats.monthlyBars, height = 112.dp, averageFraction = stats.monthlyAverageFraction)
                Text(stats.monthlyCaption, fontSize = 10.5.sp, lineHeight = 15.sp, color = colors.sub, modifier = Modifier.padding(top = 10.dp))
            }
        }

        if (stats.categoryBars.isNotEmpty()) {
            item(key = "where", contentType = "card") {
                StatsCard(title = "Where it goes") {
                    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        stats.categoryBars.forEach { CategoryBar(it) }
                    }
                }
            }
        }

        if (stats.movers.isNotEmpty()) {
            item(key = "movers", contentType = "card") {
                StatsCard(title = "Biggest changes") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        stats.movers.forEach { MoverRow(it) }
                    }
                    Text("Compared with the previous period.", fontSize = 10.sp, color = colors.sub, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }

        if (stats.topEntries.isNotEmpty()) {
            item(key = "top-entries", contentType = "card") {
                StatsCard(title = "Top entries") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        stats.topEntries.forEachIndexed { index, entry -> TopEntryRow(index + 1, entry) }
                    }
                }
            }
        }

        if (stats.weekdayCaption.isNotEmpty()) {
            item(key = "weekday", contentType = "card") {
                StatsCard(title = "By weekday") {
                    BarChart(stats.weekdayBars, height = 80.dp, averageFraction = null)
                    Text(stats.weekdayCaption, fontSize = 10.5.sp, lineHeight = 15.sp, color = colors.sub, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }

        item(key = "tips", contentType = "card") {
            StatsCard(title = "Tips & suggestions", gradient = true) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    stats.tips.forEach { TipRow(it) }
                }
            }
        }

        item(key = "findings", contentType = "card") {
            StatsCard(title = "Findings", gradient = true) {
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    stats.findings.forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.padding(top = 5.dp).size(6.dp).background(colors.chartFill, RoundedCornerShape(50)))
                            Text(line, fontSize = 11.5.sp, lineHeight = 16.7.sp, color = colors.mink)
                        }
                    }
                }
            }
        }

        if (stats.showComparison) {
            item(key = "versus", contentType = "versus") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.darkGradient, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                ) {
                    Text("Versus the previous period", fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f))
                    Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text(stats.currentLabel, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.6f))
                            Text(stats.currentValue, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(top = 3.dp))
                        }
                        Column {
                            Text(stats.previousLabel, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.6f))
                            Text(stats.previousValue, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(title: String, gradient: Boolean = false, content: @Composable () -> Unit) {
    val colors = SyncSpendTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (gradient) colors.cardGradient else androidx.compose.ui.graphics.SolidColor(colors.card), shape)
            .border(1.dp, colors.line, shape)
            .padding(16.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
            color = if (gradient) colors.mink else colors.ink,
        )
        Column(modifier = Modifier.padding(top = 12.dp)) { content() }
    }
}

/** Vertical bars that grow up from the baseline once when they first appear or their data changes. */
@Composable
private fun BarChart(bars: List<BarUi>, height: Dp, averageFraction: Float?) {
    val colors = SyncSpendTheme.colors
    val progress = remember { Animatable(0f) }
    LaunchedEffect(bars) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    val lineColor = colors.sub.copy(alpha = 0.55f)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .drawBehind {
                    // Dashed average line, drawn behind the bars.
                    if (averageFraction != null) {
                        val y = size.height * (1f - averageFraction)
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                        )
                    }
                },
        ) {
            bars.forEach { bar ->
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 5.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(bar.fraction.coerceAtLeast(0.03f))
                            .graphicsLayer {
                                scaleY = progress.value
                                transformOrigin = TransformOrigin(0.5f, 1f)
                            }
                            .background(
                                if (bar.highlighted) colors.chartFill else colors.chartFillDim,
                                RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 2.dp, bottomEnd = 2.dp),
                            ),
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            bars.forEach { bar ->
                Text(
                    bar.label,
                    fontSize = 10.sp,
                    color = if (bar.highlighted) colors.ink else colors.sub,
                    fontWeight = if (bar.highlighted) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CategoryBar(bar: CategoryBarUi) {
    val colors = SyncSpendTheme.colors
    val progress = remember { Animatable(0f) }
    LaunchedEffect(bar) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(28.dp).background(colors.tile, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(SyncSpendIcons.iconFor(bar.iconKey), null, tint = colors.ink, modifier = Modifier.size(14.dp)) }
            Text(bar.name, style = MaterialTheme.typography.bodyMedium, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(bar.totalFormatted, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
            Text("${bar.sharePercent}%", fontSize = 11.sp, color = colors.sub, modifier = Modifier.padding(start = 2.dp))
        }
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .fillMaxWidth()
                .height(6.dp)
                .background(colors.tile, RoundedCornerShape(50)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(bar.sharePercent.coerceIn(2, 100) / 100f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = progress.value
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .background(colors.chartFill, RoundedCornerShape(50)),
            )
        }
    }
}

@Composable
private fun TipRow(tip: TipUi) {
    val colors = SyncSpendTheme.colors
    val (tint, icon) = when (tip.tone) {
        TipTone.WATCH -> colors.neg to SyncSpendIcons.Bolt
        TipTone.GOOD -> colors.mink to SyncSpendIcons.Trend
        TipTone.INFO -> colors.sub to SyncSpendIcons.Spark
    }
    // A good-news tip sits on the mint gradient like every other green surface.
    val chip = if (tip.tone == TipTone.GOOD) colors.chartFill else androidx.compose.ui.graphics.SolidColor(tint.copy(alpha = 0.14f))
    Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        Box(
            modifier = Modifier.size(28.dp).background(chip, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(tip.title, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.mink)
            Text(tip.body, fontSize = 11.5.sp, lineHeight = 16.sp, color = colors.msub, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** Fill = spent so far, pale extension = where the month is heading, tick = last month's total. */
@Composable
private fun PaceBar(pace: PaceUi) {
    val colors = SyncSpendTheme.colors
    val progress = remember { Animatable(0f) }
    LaunchedEffect(pace) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    val tick = colors.ink.copy(alpha = 0.7f)
    Column {
        Text(pace.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.ink)
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(10.dp)
                .background(colors.tile, RoundedCornerShape(50))
                .drawWithContent {
                    drawContent()
                    pace.lastMonthFraction?.let { f ->
                        val x = size.width * f
                        drawLine(tick, Offset(x, -3.dp.toPx()), Offset(x, size.height + 3.dp.toPx()), strokeWidth = 2.dp.toPx())
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pace.projectedFraction.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = progress.value
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .background(colors.chartFillDim, RoundedCornerShape(50)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(pace.soFarFraction.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = progress.value
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .background(colors.chartFill, RoundedCornerShape(50)),
            )
        }
        Text(pace.caption, fontSize = 10.5.sp, lineHeight = 15.sp, color = colors.sub, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun MoverRow(mover: MoverUi) {
    val colors = SyncSpendTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(28.dp).background(colors.tile, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(SyncSpendIcons.iconFor(mover.iconKey), null, tint = colors.ink, modifier = Modifier.size(14.dp)) }
        Text(mover.name, style = MaterialTheme.typography.bodyMedium, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        // Same rule as the tiles: an improvement sits on the mint gradient, a worsening stays red.
        Row(
            modifier = if (mover.good) Modifier.mintChip() else Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                if (mover.up) SyncSpendIcons.Up else SyncSpendIcons.Down,
                null,
                tint = if (mover.good) colors.mink else colors.neg,
                modifier = Modifier.size(12.dp),
            )
            Text(
                mover.deltaFormatted,
                style = MaterialTheme.typography.bodyMedium,
                color = if (mover.good) colors.mink else colors.neg,
            )
        }
    }
}

/** A small pill filled with the mint gradient (the Top spending card's green) for text that reads as "good". */
@Composable
private fun Modifier.mintChip(): Modifier =
    background(SyncSpendTheme.colors.chartFill, RoundedCornerShape(10.dp)).padding(horizontal = 9.dp, vertical = 2.dp)

@Composable
private fun TopEntryRow(rank: Int, entry: TopEntryUi) {
    val colors = SyncSpendTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(28.dp).background(colors.chartFill, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) { Text(rank.toString(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.mink) }
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.bodyMedium, color = colors.ink, maxLines = 1)
            Text(entry.subtitle, fontSize = 10.5.sp, color = colors.sub, maxLines = 1, modifier = Modifier.padding(top = 1.dp))
        }
        Text(entry.amount, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
    }
}
