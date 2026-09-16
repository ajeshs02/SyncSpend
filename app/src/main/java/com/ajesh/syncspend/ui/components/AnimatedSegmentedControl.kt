package com.ajesh.syncspend.ui.components

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.theme.SyncSpendCorners
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Reusable segmented control where a single indicator pill *slides* from the
 * previously-selected segment to the newly-selected one, rather than each
 * option instantly recoloring itself (the design's original per-option
 * background swap). Used at every toggle site in the app: Expense/Income,
 * Entries/Categories/Stats, Light/Dark/System, Year/All-time, AM/PM.
 *
 * How the slide works: every label reports its own laid-out bounds (via
 * [onGloballyPositioned] against this composable's own coordinate space), and
 * a pair of [Animatable]s (x offset + width, both in px) animate toward the
 * newly-selected label's bounds whenever [selectedIndex] changes. The very
 * first composition has no prior bounds to animate from, so it snaps instead
 * of sliding in from nowhere.
 */
@Composable
fun AnimatedSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    trackColor: Color = SyncSpendTheme.colors.pill,
    trackBorderColor: Color = SyncSpendTheme.colors.line,
    indicatorBrush: Brush = SyncSpendTheme.colors.darkGradient,
    selectedContentColor: Color = Color.White,
    unselectedContentColor: Color = SyncSpendTheme.colors.sub,
    textStyle: TextStyle = androidx.compose.material3.MaterialTheme.typography.labelLarge,
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
            launch { offsetX.animateTo(target.left, tween(250, easing = FastOutSlowInEasing)) }
            launch { widthPx.animateTo(target.width, tween(250, easing = FastOutSlowInEasing)) }
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
            val indicatorWidth = with(LocalDensity.current) { widthPx.value.toDp() }
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .width(indicatorWidth)
                    .fillMaxHeight()
                    .background(indicatorBrush, innerShape),
            ) {}
        }
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
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
                    Text(
                        text = label,
                        style = textStyle,
                        color = if (selected) selectedContentColor else unselectedContentColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
