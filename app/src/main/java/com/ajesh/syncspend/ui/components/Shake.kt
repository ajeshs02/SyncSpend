package com.ajesh.syncspend.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * A short side-to-side shake that plays each time [trigger] changes (0 = never), and only when [active].
 * Used to point at what is missing after a tap on a button that could not go ahead. The offset is read in
 * the graphics layer, so nothing recomposes while it plays.
 */
@Composable
fun Modifier.shakeWhen(trigger: Int, active: Boolean): Modifier {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0 && active) {
            offset.animateTo(0f, keyframes {
                durationMillis = 320
                -12f at 50
                12f at 110
                -8f at 170
                8f at 230
                -3f at 280
            })
        }
    }
    return graphicsLayer { translationX = offset.value * density }
}
