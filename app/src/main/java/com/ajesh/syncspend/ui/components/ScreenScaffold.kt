package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared spacing convention so every screen's scrollable content clears the
 * floating bottom nav (never hidden behind it — always scrollable into view)
 * and starts with breathing room below the status bar for one-handed reach.
 * Both values include the real system-bar insets, so they hold on gesture-nav
 * and 3-button-nav devices alike.
 */
object SyncSpendChrome {
    /** Status bar + the design's 14dp + 8dp extra so content isn't jammed at the very top. */
    val screenTopInset: Dp
        @Composable get() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 22.dp

    /** Navigation bar + the pill's footprint + breathing room, so the last item scrolls clear of the nav. */
    val screenBottomContentPadding: Dp
        @Composable get() = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 124.dp

    /** Gap between the pill and the navigation bar. */
    val bottomBarBottomInset = 14.dp
    val bottomFadeHeight = 132.dp
}
