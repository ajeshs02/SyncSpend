package com.ajesh.syncspend.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import com.ajesh.syncspend.util.CurrencyFormatter

/**
 * A money figure that plays a one-time cascading count-up (see [CountUp]) and is
 * otherwise plain text. [playOnce] is read only when this first composes — the
 * caller passes "has the launch animation not run yet in this process?" — and
 * [onStart] tells it to record that the animation has begun, so returning to the
 * screen, switching Expense/Income or changing the month just shows the figure.
 * The animation waits for [ready] (real data) so it never counts up to a placeholder.
 */
@Composable
fun CascadingAmountText(
    target: Double,
    ready: Boolean,
    playOnce: Boolean,
    onStart: () -> Unit,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) {
    val armed = remember { playOnce }
    var finished by remember { mutableStateOf(!armed) }
    val elapsed = remember { Animatable(0f) }

    LaunchedEffect(ready) {
        if (!ready || finished) return@LaunchedEffect
        onStart()
        elapsed.animateTo(CountUp.TOTAL_MS, tween(CountUp.TOTAL_MS.toInt(), easing = LinearEasing))
        finished = true
    }

    val finalText = remember(target) { CurrencyFormatter.amount(target) }
    val text = if (finished) {
        buildAnnotatedString { append(finalText) }
    } else {
        buildAnnotatedString {
            CountUp.frame(finalText, elapsed.value).forEach { cell ->
                // Leading zeros stay in the layout but are invisible, so the figure doesn't jitter.
                if (cell.visible) append(cell.char) else withStyle(SpanStyle(color = Color.Transparent)) { append(cell.char) }
            }
        }
    }
    Text(
        text = text,
        // Its own layer: the per-frame text change re-records just this layer, not the whole screen.
        modifier = modifier.graphicsLayer(),
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        // Same-width digits (where the font has them) stop the figure jittering while it counts.
        style = TextStyle(fontFeatureSettings = "tnum"),
    )
}
