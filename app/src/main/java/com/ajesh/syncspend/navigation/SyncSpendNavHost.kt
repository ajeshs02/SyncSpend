package com.ajesh.syncspend.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ajesh.syncspend.ui.addentry.AddEntryScreen
import com.ajesh.syncspend.ui.categories.CategoriesScreen
import com.ajesh.syncspend.ui.home.HomeScreen
import com.ajesh.syncspend.ui.settings.SettingsScreen
import com.ajesh.syncspend.ui.subsreminders.SubsRemindersScreen
import com.ajesh.syncspend.ui.transactions.StatsScreen
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
                onOpenStats = { navController.navigateToTab(Routes.STATS) },
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
        composable(Routes.STATS) { StatsScreen() }
        composable(Routes.SETTINGS) { SettingsScreen(onOpenCategories = { navController.navigateToCategories() }) }
        composable(
            route = Routes.SUBS_REMINDERS,
            arguments = listOf(
                navArgument("listMode") { type = NavType.StringType },
                navArgument("highlight") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            SubsRemindersScreen(
                listMode = entry.arguments?.getString("listMode") ?: "subs",
                highlightId = entry.arguments?.getLong("highlight")?.takeIf { it > 0 },
                onBack = { navController.popBackStack() },
            )
        }
    }

    // The swipe-back gesture used to drive NavHost's own "predictive back" preview, which scrubs those
    // (differently timed) transitions with your finger and looked broken. Owning the callback here makes
    // the gesture exactly the same plain pop as the top-left back arrow. A handler registered after the
    // NavHost's own one is asked first; on Home nothing is enabled, so the system handles back as usual.
    val currentEntry by navController.currentBackStackEntryAsState()
    val canGoBack = currentEntry != null && navController.previousBackStackEntry != null
    BackHandler(enabled = canGoBack) {
        if (currentEntry?.destination?.route == Routes.CATEGORIES) navController.popFromCategories()
        else navController.popBackStack()
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

/**
 * Categories is only ever reached by pushing it from Settings, so unlike the tab switch above it
 * must leave Settings itself on the back stack. Round-tripping through Settings would otherwise
 * cold-start a fresh CategoriesViewModel (and an empty-state flash) on every visit; saving and
 * restoring just this one destination's state keeps it resident like a tab, without touching
 * anything below it and without any extra fade/gate in the screen itself.
 */
fun NavHostController.navigateToCategories() {
    navigate(Routes.CATEGORIES) { launchSingleTop = true; restoreState = true }
}

/** The other half of [navigateToCategories]: leaves Categories's state saved so the next visit restores it. */
fun NavHostController.popFromCategories() {
    popBackStack(Routes.CATEGORIES, inclusive = true, saveState = true)
}

/**
 * Opens the Subscriptions ("subs") or Reminders ("alerts") page from anywhere, with [highlightId] on top.
 * Starts from Home so Back returns there; if a subscriptions/reminders page is already open it is reused
 * rather than stacked.
 */
fun NavHostController.openSubsReminders(listMode: String, highlightId: Long?) {
    val onSubsPage = currentDestination?.route?.startsWith(Routes.SUBS_REMINDERS_PREFIX) == true
    if (!onSubsPage) navigateToTab(Routes.HOME)
    navigate(Routes.subsReminders(listMode, highlightId)) { launchSingleTop = true }
}
