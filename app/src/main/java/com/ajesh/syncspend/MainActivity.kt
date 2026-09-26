package com.ajesh.syncspend

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ajesh.syncspend.data.datastore.UserPreferences
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.model.ThemeMode
import com.ajesh.syncspend.navigation.Routes
import com.ajesh.syncspend.navigation.SyncSpendNavHost
import com.ajesh.syncspend.navigation.navigateToTab
import com.ajesh.syncspend.ui.components.BottomFadeAndNav
import com.ajesh.syncspend.ui.editentry.EditEntryHost
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

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
                SyncSpendTheme(themeMode = prefs.themeMode) {
                    SyncSpendAppRoot()
                }
            }
        }
    }
}

@Composable
private fun SyncSpendAppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SyncSpendTheme.colors.screenGradient),
    ) {
        SyncSpendNavHost(navController = navController, modifier = Modifier.fillMaxSize())
        BottomFadeAndNav(
            currentRoute = currentRoute,
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
