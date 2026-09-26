package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.ajesh.syncspend.R

/**
 * The launcher icon, drawn from the same two layers the adaptive icon uses, so
 * anywhere it appears (e.g. the widget panel's header) matches the app icon
 * exactly. The 108dp foreground layer is scaled so its 72dp visible window
 * fills the tile.
 */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier, radius: Dp = size * 0.32f) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(radius)),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(size * 1.5f))
    }
}
