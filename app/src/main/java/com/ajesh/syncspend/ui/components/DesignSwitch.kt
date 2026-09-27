package com.ajesh.syncspend.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/** The design's 46x27 switch: gradient track when on, sliding white knob. */
@Composable
fun DesignSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val knobX by animateDpAsState(if (checked) 19.dp else 0.dp, tween(200), label = "knob")
    val trackOn by animateFloatAsState(if (checked) 1f else 0f, tween(200), label = "track-on")
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 27.dp)
            .background(SyncSpendTheme.colors.dim, RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier
                .size(width = 46.dp, height = 27.dp)
                .graphicsLayer { alpha = trackOn }
                .background(
                    Brush.linearGradient(listOf(Color(0xFFFFCC00), Color(0xFFE6A700))),
                    RoundedCornerShape(14.dp),
                ),
        )
        Box(
            Modifier
                .offset { IntOffset((3.dp + knobX).roundToPx(), 3.dp.roundToPx()) } // read at layout time: no recomposition per frame
                .size(21.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}
