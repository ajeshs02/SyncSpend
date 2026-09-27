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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.theme.SyncSpendCorners
import com.ajesh.syncspend.ui.theme.SyncSpendPalette
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import kotlin.math.abs

/** Optional leading icon for a segment, with a tint per selection state. */
data class SegmentIcon(val vector: ImageVector, val selectedTint: Color, val unselectedTint: Color)

/**
 * Reusable segmented control where a single indicator pill *slides* from the
 * previously-selected segment to the newly-selected one, rather than each
 * option instantly recoloring itself (the design's original per-option
 * background swap). Used at every toggle site in the app: Expense/Income,
 * Entries/Categories/Stats, Light/Dark/System, AM/PM.
 *
 * Options are equal-width, so the pill's geometry is plain arithmetic
 * (`position * cellWidth`) — nothing is measured and waited for, so it is already in the right
 * place on the very first frame of a screen. One [Animatable] holds the fractional position and
 * is read only in the draw phase (the pill, and each label's colour): a slide recomposes nothing.
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
    val count = options.size
    val position = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) {
        position.animateTo(selectedIndex.toFloat(), tween(260, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(trackColor, outerShape)
            .border(1.dp, trackBorderColor, outerShape)
            .padding(4.dp),
    ) {
        // Own graphics layer: the slide only re-records this layer.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer()
                .drawBehind {
                    val cell = size.width / count
                    val left = position.value * cell
                    // inset() shrinks the draw scope to the pill, so a gradient brush spans the pill, not the track.
                    inset(left = left, top = 0f, right = size.width - left - cell, bottom = 0f) {
                        drawOutline(innerShape.createOutline(size, layoutDirection, this), indicatorBrush)
                    }
                },
        )
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { index, label ->
                val icon = icons?.getOrNull(index)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            role = Role.Tab
                            selected = index == selectedIndex
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            SegmentGlyph(icon, index, position, iconSize)
                            Box(Modifier.width(5.dp))
                        }
                        BasicText(
                            text = label,
                            style = textStyle.copy(textAlign = TextAlign.Center),
                            // Colour follows the pill: read in the draw phase, so it never lags or recomposes.
                            color = { lerp(unselectedContentColor, selectedContentColor, closeness(position.value, index)) },
                        )
                    }
                }
            }
        }
    }
}

/** 1 when the pill is exactly over segment [index], fading to 0 one segment away. */
internal fun closeness(position: Float, index: Int): Float = (1f - abs(position - index)).coerceIn(0f, 1f)

/** A segment's icon in its selected and unselected tints, cross-faded by how far the pill is from it. */
@Composable
private fun SegmentGlyph(icon: SegmentIcon, index: Int, position: Animatable<Float, *>, size: Dp) {
    Box {
        Icon(
            icon.vector, null, tint = icon.unselectedTint,
            modifier = Modifier.size(size).graphicsLayer { alpha = 1f - closeness(position.value, index) },
        )
        Icon(
            icon.vector, null, tint = icon.selectedTint,
            modifier = Modifier.size(size).graphicsLayer { alpha = closeness(position.value, index) },
        )
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
            SegmentIcon(com.ajesh.syncspend.ui.icons.SyncSpendIcons.ArrowIn, SyncSpendPalette.BrandGold, SyncSpendPalette.BrandGold),
        )
    }
}
