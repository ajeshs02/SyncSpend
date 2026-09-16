package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/** The 4 tab destinations, in the order they render either side of the center Add button. */
enum class NavDestination(val route: String) {
    Home("home"),
    Transactions("transactions"),
    Categories("categories"),
    Settings("settings"),
}

private val NavPillDark = Color(0xFF050806)
private val NavActiveTint = Color(0xFF8ECF63)
private val NavInactiveTint = Color(0x7AFFFFFF) // rgba(255,255,255,.48)
private val NavAddInactiveTint = Color(0x99FFFFFF) // rgba(255,255,255,.6)

/**
 * The bottom fade scrim + floating pill nav, rendered once at the app's root
 * container (not per-screen) so it stays put across every destination,
 * including Add Entry — the design keeps the nav visible everywhere and only
 * true modals/sheets paint above it.
 */
@Composable
fun BottomFadeAndNav(
    currentRoute: String?,
    onNavigate: (NavDestination) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SyncSpendChrome.bottomFadeHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, SyncSpendTheme.colors.scrEnd),
                        startY = 0f,
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = SyncSpendChrome.bottomBarBottomInset)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(23.dp), clip = false)
                .background(NavPillDark, RoundedCornerShape(23.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavIconButton(NavDestination.Home, currentRoute) { onNavigate(NavDestination.Home) }
            NavIconButton(NavDestination.Transactions, currentRoute) { onNavigate(NavDestination.Transactions) }
            AddButton(active = currentRoute == "add_entry", onClick = onAddClick)
            NavIconButton(NavDestination.Categories, currentRoute) { onNavigate(NavDestination.Categories) }
            NavIconButton(NavDestination.Settings, currentRoute) { onNavigate(NavDestination.Settings) }
        }
    }
}

@Composable
private fun NavIconButton(destination: NavDestination, currentRoute: String?, onClick: () -> Unit) {
    val active = currentRoute == destination.route
    val icon = when (destination) {
        NavDestination.Home -> SyncSpendIcons.Home
        NavDestination.Transactions -> SyncSpendIcons.Swap
        NavDestination.Categories -> SyncSpendIcons.Layers
        NavDestination.Settings -> SyncSpendIcons.Cog
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 40.dp)
            .scale(if (pressed) 0.9f else 1f)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = destination.name,
            tint = if (active) NavActiveTint else NavInactiveTint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AddButton(active: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 40.dp)
            .scale(if (pressed) 0.9f else 1f)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = SyncSpendIcons.Plus,
            contentDescription = "Add entry",
            tint = if (active) NavActiveTint else NavAddInactiveTint,
            modifier = Modifier.size(25.dp),
        )
    }
}
