package com.ajesh.syncspend.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.theme.SyncSpendCorners
import com.ajesh.syncspend.ui.theme.SyncSpendPalette
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Optional leading icon for a segment, with a tint per selection state. */
data class SegmentIcon(val vector: ImageVector, val selectedTint: Color, val unselectedTint: Color)

/**
 * Reusable segmented control where a single indicator pill *slides* from the
 * previously-selected segment to the newly-selected one, rather than each
 * option instantly recoloring itself (the design's original per-option
 * background swap). Used at every toggle site in the app: Expense/Income,
 * Entries/Categories/Stats, Light/Dark/System, AM/PM.
 *
 * Every label reports its laid-out bounds; a pair of [Animatable]s (x offset +
 * width) animate toward the newly-selected label whenever [selectedIndex]
 * changes. The very first placement snaps instead of sliding in from nowhere.
 * Label colors cross-fade with the indicator so text never lags behind it.
 */
@Composable
fun AnimatedSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    icons: List<SegmentIcon?>? = null,
    iconSize: Dp = 15.dp,
    trackColor: Color = SyncSpendTheme.colors.pill,
    trackBorderColor: Color = SyncSpendTheme.colors.line,
    indicatorBrush: Brush = SyncSpendTheme.colors.selectedBrush,
    selectedContentColor: Color = SyncSpendTheme.colors.onSelected,
    unselectedContentColor: Color = SyncSpendTheme.colors.sub,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    outerShape: Shape = SyncSpendCorners.pillOuter,
    innerShape: Shape = SyncSpendCorners.pillInner,
) {
    val bounds = remember(options.size) { mutableStateListOf<Rect?>().apply { repeat(options.size) { add(null) } } }
    val offsetX = remember { Animatable(0f) }
    val widthPx = remember { Animatable(0f) }
    var hasPlacedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIndex, bounds.getOrNull(selectedIndex)) {
        val target = bounds.getOrNull(selectedIndex) ?: return@LaunchedEffect
        if (!hasPlacedOnce) {
            offsetX.snapTo(target.left)
            widthPx.snapTo(target.width)
            hasPlacedOnce = true
        } else {
            launch { offsetX.animateTo(target.left, tween(260, easing = FastOutSlowInEasing)) }
            launch { widthPx.animateTo(target.width, tween(260, easing = FastOutSlowInEasing)) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(trackColor, outerShape)
            .border(1.dp, trackBorderColor, outerShape)
            .padding(4.dp),
    ) {
        if (hasPlacedOnce) {
            // Width and offset are read in the layout/placement phases, so a slide
            // re-lays-out one box per frame instead of recomposing the control.
            Box(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val w = widthPx.value.roundToInt().coerceAtLeast(0)
                        val placeable = measurable.measure(Constraints.fixed(w, constraints.maxHeight))
                        layout(w, placeable.height) { placeable.place(offsetX.value.roundToInt(), 0) }
                    }
                    .background(indicatorBrush, innerShape),
            )
        }
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(
                    if (selected) selectedContentColor else unselectedContentColor,
                    tween(200),
                    label = "seg-text",
                )
                val icon = icons?.getOrNull(index)
                val iconColor by animateColorAsState(
                    if (selected) icon?.selectedTint ?: Color.Unspecified else icon?.unselectedTint ?: Color.Unspecified,
                    tween(200),
                    label = "seg-icon",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { coords -> bounds[index] = coords.boundsInParent() }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            Icon(icon.vector, null, tint = iconColor, modifier = Modifier.size(iconSize))
                            Box(Modifier.width(5.dp))
                        }
                        Text(text = label, style = textStyle, color = textColor, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

/**
 * The Expense / Income toggle used on Home, Add Entry, Categories and Edit Entry:
 * taller than the app's other segmented controls (it's the most-used control).
 */
@Composable
fun FlowToggle(
    type: FlowType,
    onSelect: (FlowType) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedSegmentedControl(
        options = listOf("Expense", "Income"),
        selectedIndex = if (type == FlowType.EXPENSE) 0 else 1,
        onSelect = { onSelect(if (it == 0) FlowType.EXPENSE else FlowType.INCOME) },
        icons = flowSegmentIcons(),
        height = 50.dp,
        iconSize = 16.dp,
        textStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
        modifier = modifier,
    )
}

/** The design's Expense / Income icon pair, tinted so it reads on either indicator theme. */
@Composable
fun flowSegmentIcons(): List<SegmentIcon> {
    val c = SyncSpendTheme.colors
    return remember(c) {
        listOf(
            SegmentIcon(com.ajesh.syncspend.ui.icons.SyncSpendIcons.ArrowOut, c.expenseOnSelected, c.neg),
            SegmentIcon(com.ajesh.syncspend.ui.icons.SyncSpendIcons.ArrowIn, SyncSpendPalette.BrandGreen, SyncSpendPalette.BrandGreen),
        )
    }
}
