package com.ajesh.syncspend.ui.components

import androidx.compose.ui.unit.dp

/**
 * Shared spacing convention so every screen's scrollable content clears the
 * floating bottom nav (user requirement: never let content hide behind it —
 * always scrollable into view) and starts with a bit of top breathing room
 * for one-handed reachability (user requirement: screens don't need to start
 * flush at the very top).
 */
object SyncSpendChrome {
    /** Matches the design's own ~124px reserved bottom padding on scroll areas. */
    val screenBottomContentPadding = 124.dp
    val screenTopInset = 18.dp
    val bottomBarHeight = 40.dp
    val bottomBarBottomInset = 26.dp
    val bottomFadeHeight = 120.dp
}
