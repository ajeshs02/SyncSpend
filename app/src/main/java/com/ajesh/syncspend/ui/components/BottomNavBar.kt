package com.ajesh.syncspend.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendPalette
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import kotlin.math.roundToInt

/** The 4 tab destinations, in the order they render either side of the center Add button. */
enum class NavDestination(val route: String) {
    Home("home"),
    Transactions("transactions"),
    Categories("categories"),
    Settings("settings"),
}

private val NavPillDark = Color(0xFF050806)
private val NavActiveTint = SyncSpendPalette.BrandGreen
private val NavInactiveTint = Color(0x7AFFFFFF) // rgba(255,255,255,.48)
private val NavAddInactiveTint = Color(0x99FFFFFF) // rgba(255,255,255,.6)

// Pill geometry. Five cells, the centre Add cell wider and with a bigger plus so it reads as the
// key button: 4 x 56 + 72 + 2 x 10 padding = 316dp.
private val CellHeight = 50.dp
private val PillPadding = 10.dp
private val PillVerticalPadding = 6.dp
private val PillRadius = 31.dp
internal val NavCellWidth = 56.dp
internal val NavAddCellWidth = 72.dp
internal const val NAV_ADD_SLOT = 2

/** Slot index in the pill for [route], or -1 when no tab is current (Subscriptions, Reminders). */
internal fun navSlotFor(route: String?): Int = when (route) {
    NavDestination.Home.route -> 0
    NavDestination.Transactions.route -> 1
    "add_entry" -> NAV_ADD_SLOT
    NavDestination.Categories.route -> 3
    NavDestination.Settings.route -> 4
    else -> -1
}

internal fun navSlotWidth(slot: Int): Dp = if (slot == NAV_ADD_SLOT) NavAddCellWidth else NavCellWidth

/** Where slot [slot] starts: the widths of every cell before it (the Add cell is wider than the rest). */
internal fun navSlotOffset(slot: Int): Dp = (0 until slot).fold(0.dp) { acc, i -> acc + navSlotWidth(i) }

/**
 * The bottom fade scrim + floating pill nav, rendered once at the app's root
 * container (not per-screen) so it stays put across every destination,
 * including Add Entry — the design keeps the nav visible everywhere and only
 * true modals/sheets paint above it. The design's soft fill under the active
 * icon is a single indicator that slides between tabs.
 */
@Composable
fun BottomFadeAndNav(
    currentRoute: String?,
    onNavigate: (NavDestination) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SyncSpendChrome.bottomFadeHeight + navInset)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, SyncSpendTheme.colors.scrEnd),
                        startY = 0f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = SyncSpendChrome.bottomBarBottomInset)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(PillRadius), clip = false)
                .background(NavPillDark, RoundedCornerShape(PillRadius))
                .padding(horizontal = PillPadding, vertical = PillVerticalPadding),
        ) {
            val slot = navSlotFor(currentRoute)
            ActiveIndicator(slot)
            Row(verticalAlignment = Alignment.CenterVertically) {
                NavIconButton(NavDestination.Home, SyncSpendIcons.Home, "Home", slot == 0) { onNavigate(NavDestination.Home) }
                NavIconButton(NavDestination.Transactions, SyncSpendIcons.Swap, "Transactions", slot == 1) { onNavigate(NavDestination.Transactions) }
                AddButton(active = slot == NAV_ADD_SLOT, onClick = onAddClick)
                NavIconButton(NavDestination.Categories, SyncSpendIcons.Layers, "Categories", slot == 3) { onNavigate(NavDestination.Categories) }
                NavIconButton(NavDestination.Settings, SyncSpendIcons.Cog, "Settings", slot == 4) { onNavigate(NavDestination.Settings) }
            }
        }
    }
}

/**
 * One soft pill that glides behind the current tab and fades away when no tab
 * is current. Position and alpha are read in the layout/draw phases, so the
 * slide never recomposes the nav.
 */
@Composable
private fun ActiveIndicator(slot: Int) {
    // Keep the last real slot while hidden so the pill fades out in place instead of flying off.
    // A plain holder (not snapshot state): it only remembers the last tab for the fade-out.
    val last = remember { IntArray(1) }
    if (slot >= 0) last[0] = slot
    val x by animateDpAsState(navSlotOffset(last[0]), tween(280, easing = FastOutSlowInEasing), label = "nav-x")
    val width by animateDpAsState(navSlotWidth(last[0]), tween(280, easing = FastOutSlowInEasing), label = "nav-w")
    val visible by animateFloatAsState(if (slot >= 0) 1f else 0f, tween(180), label = "nav-visible")
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { IntOffset(with(density) { x.toPx() }.roundToInt(), 0) }
            .size(width = width, height = CellHeight)
            .graphicsLayer { alpha = visible }
            .padding(horizontal = 3.dp, vertical = 3.dp)
            .background(NavActiveTint.copy(alpha = 0.16f), RoundedCornerShape(PillRadius - PillVerticalPadding - 3.dp)),
    )
}

@Composable
private fun NavIconButton(destination: NavDestination, icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val tint by animateColorAsState(if (active) NavActiveTint else NavInactiveTint, tween(200), label = "nav-tint")
    val press by animateFloatAsState(if (pressed) 0.9f else 1f, tween(90), label = "nav-press")
    Box(
        modifier = Modifier
            .size(width = NavCellWidth, height = CellHeight)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .semantics {
                contentDescription = label
                role = Role.Tab
                selected = active
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun AddButton(active: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val tint by animateColorAsState(if (active) NavActiveTint else NavAddInactiveTint, tween(200), label = "nav-add-tint")
    val press by animateFloatAsState(if (pressed) 0.9f else 1f, tween(90), label = "nav-add-press")
    Box(
        modifier = Modifier
            .size(width = NavAddCellWidth, height = CellHeight)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .semantics {
                contentDescription = "Add entry"
                role = Role.Tab
                selected = active
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = SyncSpendIcons.Plus, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
    }
}
