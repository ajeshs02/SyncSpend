package com.ajesh.syncspend.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ajesh.syncspend.ui.addentry.AddEntryScreen
import com.ajesh.syncspend.ui.categories.CategoriesScreen
import com.ajesh.syncspend.ui.home.HomeScreen
import com.ajesh.syncspend.ui.settings.SettingsScreen
import com.ajesh.syncspend.ui.subsreminders.SubsRemindersScreen
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
    // The design's `ssup` entry — a fade with a small rise — kept short (220ms) because the new
    // screen composes while it runs; exits are a very quick fade so the outgoing screen never
    // fights the incoming one.
    val enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 40 }
    val exit = fadeOut(tween(90))
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = { enter },
        exitTransition = { exit },
        popEnterTransition = { enter },
        popExitTransition = { exit },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onViewAllTransactions = { navController.navigateToTab(Routes.TRANSACTIONS) },
                onOpenSubscriptions = { navController.navigate(Routes.subsReminders("subs")) },
                onOpenReminders = { navController.navigate(Routes.subsReminders("alerts")) },
            )
        }
        composable(Routes.TRANSACTIONS) { TransactionsScreen() }
        composable(Routes.ADD_ENTRY) {
            AddEntryScreen(
                onBack = { navController.navigateToTab(Routes.HOME) },
                onSaved = { navController.navigateToTab(Routes.HOME) },
            )
        }
        composable(Routes.CATEGORIES) { CategoriesScreen() }
        composable(Routes.SETTINGS) { SettingsScreen() }
        composable(
            route = Routes.SUBS_REMINDERS,
            arguments = listOf(navArgument("listMode") { type = NavType.StringType }),
        ) { entry ->
            SubsRemindersScreen(
                listMode = entry.arguments?.getString("listMode") ?: "subs",
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * Standard single-stack bottom-nav navigation: no back-stack pileup between
 * tabs. A pushed detail screen (Subscriptions / Reminders, opened from Home) is
 * popped first, otherwise `popUpTo{saveState}` would save it as part of Home's
 * state and `restoreState` would bring it back the next time Home is tapped.
 */
fun NavHostController.navigateToTab(route: String) {
    if (currentDestination?.route?.startsWith(Routes.SUBS_REMINDERS_PREFIX) == true) popBackStack()
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
