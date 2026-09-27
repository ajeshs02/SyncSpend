package com.ajesh.syncspend.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner radii lifted directly from the design's border-radius values. */
object SyncSpendCorners {
    val pillOuter = RoundedCornerShape(16.dp) // segmented-control track
    val pillInner = RoundedCornerShape(12.dp) // segmented-control indicator/option
    val card = RoundedCornerShape(18.dp)
    val cardLarge = RoundedCornerShape(20.dp)
    val hero = RoundedCornerShape(24.dp)
    val tile = RoundedCornerShape(11.dp)
    val chip = RoundedCornerShape(15.dp)
    val sheetTop = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    val dialog = RoundedCornerShape(22.dp)
    val circle = RoundedCornerShape(50)
    /** Fully-rounded pill: the floating bottom nav, and any other fully-rounded control that joins it. */
    val pill = RoundedCornerShape(31.dp)
}

val SyncSpendShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = SyncSpendCorners.card,
    large = SyncSpendCorners.hero,
    extraLarge = SyncSpendCorners.sheetTop,
)
