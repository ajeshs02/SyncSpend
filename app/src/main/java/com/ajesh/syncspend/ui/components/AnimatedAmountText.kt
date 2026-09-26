package com.ajesh.syncspend.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.ajesh.syncspend.util.CurrencyFormatter

/**
 * A money figure that counts up to [target] instead of jumping: from zero when
 * it first has real data ([ready]), and from the value currently on screen
 * whenever [target] changes afterwards. Progress is a 0..1 float and the
 * interpolation is done in Double — a Float would lose cents on 8-9 digit
 * totals. Kept as its own composable so only this Text recomposes per frame.
 */
@Composable
fun AnimatedAmountText(
    target: Double,
    ready: Boolean,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    durationMillis: Int = 700,
) {
    val progress = remember { Animatable(1f) }
    val from = remember { mutableDoubleStateOf(0.0) }
    val to = remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(target, ready) {
        if (!ready) return@LaunchedEffect
        from.doubleValue = from.doubleValue + (to.doubleValue - from.doubleValue) * progress.value
        to.doubleValue = target
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis, easing = FastOutSlowInEasing))
    }

    val shown = from.doubleValue + (to.doubleValue - from.doubleValue) * progress.value
    Text(
        text = CurrencyFormatter.amount(shown),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        // Same-width digits (where the font has them) stop the figure jittering while it counts.
        style = TextStyle(fontFeatureSettings = "tnum"),
    )
}
