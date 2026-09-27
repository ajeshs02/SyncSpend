package com.ajesh.syncspend

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ajesh.syncspend.data.datastore.UserPreferences
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.model.AppLink
import com.ajesh.syncspend.domain.model.ThemeMode
import com.ajesh.syncspend.navigation.Routes
import com.ajesh.syncspend.navigation.SyncSpendNavHost
import com.ajesh.syncspend.navigation.navigateToTab
import com.ajesh.syncspend.navigation.openSubsReminders
import com.ajesh.syncspend.ui.components.BottomFadeAndNav
import com.ajesh.syncspend.ui.editentry.EditEntryHost
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private const val SPLASH_MAX_MILLIS = 1_500L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate. The splash stays up until the shared data streams are
        // loaded (normally well under 200ms; capped so a slow disk can never trap the user on it),
        // so the first frame is complete and already in the saved theme.
        val splash = installSplashScreen()
        val startedAt = SystemClock.uptimeMillis()
        val container = (application as SyncSpendApp).container
        splash.setKeepOnScreenCondition { !container.ready.value && SystemClock.uptimeMillis() - startedAt < SPLASH_MAX_MILLIS }
        super.onCreate(savedInstanceState)
        // A rotation or process restore hands back the launch intent; only a fresh launch follows its link.
        if (savedInstanceState == null) followLink(intent)
        enableEdgeToEdge()
        setContent {
            val prefs by container.preferencesRepository.preferences.collectAsStateWithLifecycle(
                container.preferencesRepository.current() ?: UserPreferences(),
            )
            val dark = when (prefs.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // System-bar icon colors follow the in-app theme, not just the OS setting.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                )
                onDispose {}
            }
            CompositionLocalProvider(LocalAppContainer provides container) {
                SyncSpendTheme(themeMode = prefs.themeMode, colorPalette = prefs.colorPalette, fontChoice = prefs.fontChoice) {
                    SyncSpendAppRoot()
                }
            }
        }
    }

    /** The activity is singleTop: a notification tapped while the app is open arrives here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        followLink(intent)
    }

    /**
     * A tapped notification (or its "Add" button) carries where it should lead: dismiss the notification if
     * asked, and hand the destination to the navigation root, which follows it once composed.
     */
    private fun followLink(intent: Intent?) {
        val link = AppLink.parse(
            intent?.getStringExtra(AppLink.EXTRA_KIND),
            intent?.getLongExtra(AppLink.EXTRA_ID, AppLink.NO_ID) ?: AppLink.NO_ID,
        ) ?: return
        intent?.getIntExtra(AppLink.EXTRA_CANCEL_NOTIFICATION, -1)?.takeIf { it >= 0 }?.let {
            NotificationManagerCompat.from(this).cancel(it)
        }
        intent?.removeExtra(AppLink.EXTRA_KIND) // followed once: the same intent must not replay
        (application as SyncSpendApp).container.selectionState.pendingLink.value = link
    }
}

@Composable
private fun SyncSpendAppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // A tapped notification (or its "Add" button) names the page it wants; follow it once.
    val container = LocalAppContainer.current
    val link by container.selectionState.pendingLink.collectAsStateWithLifecycle()
    LaunchedEffect(link) {
        val target = link ?: return@LaunchedEffect
        when (target) {
            AppLink.AddEntry -> navController.navigateToTab(Routes.ADD_ENTRY)
            is AppLink.Subscriptions -> navController.openSubsReminders("subs", target.highlightId)
            is AppLink.Reminders -> navController.openSubsReminders("alerts", target.highlightId)
        }
        container.selectionState.pendingLink.value = null
    }

    // The bottom nav's glass material blurs whatever's drawn behind it — the NavHost content is
    // the one and only Haze source, registered once here for the whole app.
    val hazeState = rememberHazeState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Flat, not the old multi-stop gradient: the monochrome redesign keeps gradients to
            // cards/charts only (see SyncSpendTheme.colors.darkGradient/mintGradient/chartFill).
            .background(SyncSpendTheme.colors.scrEnd),
    ) {
        SyncSpendNavHost(navController = navController, modifier = Modifier.fillMaxSize().hazeSource(hazeState))
        BottomFadeAndNav(
            currentRoute = currentRoute,
            hazeState = hazeState,
            onNavigate = { destination ->
                if (currentRoute != destination.route) navController.navigateToTab(destination.route)
            },
            onAddClick = {
                if (currentRoute != Routes.ADD_ENTRY) navController.navigateToTab(Routes.ADD_ENTRY)
            },
        )
        EditEntryHost()
    }
}
