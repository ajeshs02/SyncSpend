package com.ajesh.syncspend.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner radii lifted directly from the design's border-radius values. */
object SyncSpendCorners {
    val pillOuter = RoundedCornerShape(17.dp) // segmented-control track
    val pillInner = RoundedCornerShape(13.dp) // segmented-control indicator/option
    val card = RoundedCornerShape(20.dp)
    val cardLarge = RoundedCornerShape(22.dp)
    val hero = RoundedCornerShape(26.dp)
    val tile = RoundedCornerShape(12.dp)
    val chip = RoundedCornerShape(16.dp)
    val sheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val dialog = RoundedCornerShape(24.dp)
    val circle = RoundedCornerShape(50)
    /** Fully-rounded pill: the floating bottom nav, and any other fully-rounded control that joins it. */
    val pill = RoundedCornerShape(31.dp)
}

val SyncSpendShapes = Shapes(
    extraSmall = RoundedCornerShape(11.dp),
    small = RoundedCornerShape(15.dp),
    medium = SyncSpendCorners.card,
    large = SyncSpendCorners.hero,
    extraLarge = SyncSpendCorners.sheetTop,
)
