package com.ajesh.syncspend.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendPalette
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The 4 tab destinations, in the order they render either side of the center Add button. */
enum class NavDestination(val route: String) {
    Home("home"),
    Transactions("transactions"),
    Stats("stats"),
    Settings("settings"),
}

private val NavPillDark = Color(0xFF050806)
private val NavActiveTint = SyncSpendPalette.BrandGreen
private val NavIndicatorFill = Color.White.copy(alpha = 0.10f) // neutral: the vibrant green is only ever the glyph
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
    NavDestination.Stats.route -> 3
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
 *
 * Feel: the highlighted slot is *optimistic* — it changes on the tap itself, not once the
 * destination has composed — so the pill leaves on the tap frame while the new screen is
 * still being built. Every animation here (pill slide, icon cross-fade, press scale) is read
 * in the draw/graphics-layer phase, so none of them recompose anything per frame.
 */
@Composable
fun BottomFadeAndNav(
    currentRoute: String?,
    onNavigate: (NavDestination) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val routeSlot = navSlotFor(currentRoute)
    var selected by remember { mutableIntStateOf(routeSlot) }
    LaunchedEffect(routeSlot) { selected = routeSlot }
    // If a tap never turned into a navigation, fall back to what the route says.
    val latestRouteSlot by rememberUpdatedState(routeSlot)
    LaunchedEffect(selected) {
        if (selected != latestRouteSlot) {
            delay(700)
            selected = latestRouteSlot
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val scrEnd = SyncSpendTheme.colors.scrEnd
        val fade = remember(scrEnd) { Brush.verticalGradient(colors = listOf(Color.Transparent, scrEnd.copy(alpha = 0.85f))) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SyncSpendChrome.bottomFadeHeight + navInset)
                .align(Alignment.BottomCenter)
                .background(fade),
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
            ActiveIndicator(selected)
            Row(verticalAlignment = Alignment.CenterVertically) {
                NavIconButton(SyncSpendIcons.Home, "Home", selected == 0) {
                    selected = 0
                    onNavigate(NavDestination.Home)
                }
                NavIconButton(SyncSpendIcons.Swap, "Transactions", selected == 1) {
                    selected = 1
                    onNavigate(NavDestination.Transactions)
                }
                AddButton(active = selected == NAV_ADD_SLOT) {
                    selected = NAV_ADD_SLOT
                    onAddClick()
                }
                NavIconButton(SyncSpendIcons.Trend, "Stats", selected == 3) {
                    selected = 3
                    onNavigate(NavDestination.Stats)
                }
                NavIconButton(SyncSpendIcons.Cog, "Settings", selected == 4) {
                    selected = 4
                    onNavigate(NavDestination.Settings)
                }
            }
        }
    }
}

/**
 * One soft pill that glides behind the current tab and fades away when no tab is current.
 * Position, width and alpha are [Animatable]s read only inside `drawBehind` on the indicator's
 * own graphics layer: a slide neither recomposes nor re-lays-out anything, and it does not
 * invalidate the shadowed pill it sits in.
 */
@Composable
private fun BoxScope.ActiveIndicator(slot: Int) {
    val density = LocalDensity.current
    // Keep the last real slot while hidden so the pill fades out in place instead of flying off.
    // A plain holder (not snapshot state): it only remembers the last tab for the fade-out.
    val last = remember { IntArray(1) { slot.coerceAtLeast(0) } }
    if (slot >= 0) last[0] = slot
    val x = remember { Animatable(with(density) { navSlotOffset(last[0]).toPx() }) }
    val width = remember { Animatable(with(density) { navSlotWidth(last[0]).toPx() }) }
    val alpha = remember { Animatable(if (slot >= 0) 1f else 0f) }

    LaunchedEffect(slot) {
        if (slot >= 0) {
            val slide = tween<Float>(280, easing = FastOutSlowInEasing)
            launch { x.animateTo(with(density) { navSlotOffset(slot).toPx() }, slide) }
            launch { width.animateTo(with(density) { navSlotWidth(slot).toPx() }, slide) }
            launch { alpha.animateTo(1f, tween(140)) }
        } else {
            alpha.animateTo(0f, tween(180))
        }
    }

    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer()
            .drawBehind {
                val a = alpha.value
                if (a <= 0f) return@drawBehind
                val inset = 3.dp.toPx()
                val radius = (PillRadius - PillVerticalPadding - 3.dp).toPx()
                drawRoundRect(
                    color = NavIndicatorFill,
                    topLeft = Offset(x.value + inset, inset),
                    size = Size(width.value - 2 * inset, size.height - 2 * inset),
                    cornerRadius = CornerRadius(radius, radius),
                    alpha = a,
                )
            },
    )
}

/**
 * A nav glyph in two tints stacked on each other and cross-faded through the layer alpha, so the
 * active/inactive change costs no recomposition per frame.
 */
@Composable
private fun NavGlyph(icon: ImageVector, active: Boolean, inactiveTint: Color, size: Dp) {
    val fraction = remember { Animatable(if (active) 1f else 0f) }
    LaunchedEffect(active) { fraction.animateTo(if (active) 1f else 0f, tween(200)) }
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon, contentDescription = null, tint = inactiveTint,
            modifier = Modifier.size(size).graphicsLayer { alpha = 1f - fraction.value },
        )
        Icon(
            imageVector = icon, contentDescription = null, tint = NavActiveTint,
            modifier = Modifier.size(size).graphicsLayer { alpha = fraction.value },
        )
    }
}

@Composable
private fun NavIconButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
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
        NavGlyph(icon, active, NavInactiveTint, 24.dp)
    }
}

@Composable
private fun AddButton(active: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
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
        NavGlyph(SyncSpendIcons.PlusBold, active, NavAddInactiveTint, 36.dp)
    }
}
