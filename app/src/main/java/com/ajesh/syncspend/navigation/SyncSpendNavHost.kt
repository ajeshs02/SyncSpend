package com.ajesh.syncspend.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ajesh.syncspend.ui.addentry.AddEntryScreen
import com.ajesh.syncspend.ui.categories.CategoriesScreen
import com.ajesh.syncspend.ui.home.HomeScreen
import com.ajesh.syncspend.ui.settings.SettingsScreen
import com.ajesh.syncspend.ui.transactions.TransactionsScreen

/**
 * The floating bottom nav lives outside this NavHost (see MainActivity) so it
 * survives destination changes untouched. Edit Entry, pickers, and confirm
 * dialogs are deliberately NOT routes here — they're local composable state +
 * ModalBottomSheet/Dialog inside whichever screen hosts them, avoiding an
 * extra Accompanist Navigation-Material dependency.
 */
@Composable
fun SyncSpendNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Routes.HOME, modifier = modifier) {
        composable(Routes.HOME) {
            HomeScreen(onViewAllTransactions = { navController.navigateToTab(Routes.TRANSACTIONS) })
        }
        composable(Routes.TRANSACTIONS) { TransactionsScreen() }
        composable(Routes.ADD_ENTRY) { AddEntryScreen() }
        composable(Routes.CATEGORIES) { CategoriesScreen() }
        composable(Routes.SETTINGS) { SettingsScreen() }
    }
}

/** Standard single-stack bottom-nav navigation: no back-stack pileup between tabs. */
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
