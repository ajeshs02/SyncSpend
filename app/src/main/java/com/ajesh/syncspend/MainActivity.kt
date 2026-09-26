package com.ajesh.syncspend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.navigation.Routes
import com.ajesh.syncspend.navigation.SyncSpendNavHost
import com.ajesh.syncspend.navigation.navigateToTab
import com.ajesh.syncspend.ui.components.BottomFadeAndNav
import com.ajesh.syncspend.ui.components.NavDestination
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val container = (application as SyncSpendApp).container
            CompositionLocalProvider(LocalAppContainer provides container) {
                SyncSpendTheme {
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
                if (currentRoute != destination.route) {
                    navController.navigateToTab(destination.route)
                }
            },
            onAddClick = {
                if (currentRoute != Routes.ADD_ENTRY) {
                    navController.navigateToTab(Routes.ADD_ENTRY)
                }
            },
        )
    }
}
