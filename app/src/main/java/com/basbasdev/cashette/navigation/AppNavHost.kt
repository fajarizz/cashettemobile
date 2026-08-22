package com.basbasdev.cashette.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.basbasdev.cashette.feature.AccountsScreen
import com.basbasdev.cashette.feature.AnalyticsScreen
import com.basbasdev.cashette.feature.BudgetScreen
import com.basbasdev.cashette.feature.DebtScreen
import com.basbasdev.cashette.feature.PocketsScreen
import com.basbasdev.cashette.feature.SettingsScreen
import com.basbasdev.cashette.feature.SubscriptionsScreen
import com.basbasdev.cashette.feature.chat.ChatScreen
import com.basbasdev.cashette.feature.history.HistoryScreen
import com.basbasdev.cashette.feature.home.HomeScreen
import com.basbasdev.cashette.feature.money.MoneyScreen
import com.basbasdev.cashette.ui.theme.CashetteMotion
import kotlinx.coroutines.launch

/**
 * The signed-in graph. Owns its own [NavHostController], separate from the one the auth
 * graph uses, so signing out destroys this back stack rather than leaving it reachable.
 */
@Composable
fun AppNavHost(
    displayName: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Hoisted so reselecting a tab can scroll its list to top from the bar, which sits
    // outside the screen that owns the list.
    val homeListState = rememberLazyListState()
    val moneyListState = rememberLazyListState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = TopLevelDestination.entries.firstOrNull { destination ->
        backStackEntry?.destination?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
    }

    val back: () -> Unit = { navController.popBackStack() }

    val scrollingList = when (currentTab) {
        TopLevelDestination.HOME -> homeListState
        TopLevelDestination.MONEY -> moneyListState
        else -> null
    }

    AppScaffold(
        fabVisible = scrollingList?.let { rememberScrollingUp(it) } ?: true,
        currentTab = currentTab,
        onSelectTab = { destination ->
            if (destination == currentTab) {
                // Android convention: a second tap on the current tab returns you to the
                // top of it rather than doing nothing.
                scope.launch {
                    when (destination) {
                        TopLevelDestination.HOME -> homeListState.animateScrollToItem(0)
                        TopLevelDestination.MONEY -> moneyListState.animateScrollToItem(0)
                        else -> Unit
                    }
                }
            } else {
                navController.switchTab(destination)
            }
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            enterTransition = { screenEnter(pop = false) },
            exitTransition = { screenExit(pop = false) },
            popEnterTransition = { screenEnter(pop = true) },
            popExitTransition = { screenExit(pop = true) },
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    displayName = displayName,
                    listState = homeListState,
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenAnalytics = { navController.navigate(AnalyticsRoute) },
                    onOpenBudget = { navController.navigate(BudgetRoute) },
                    onOpenSubscriptions = { navController.navigate(SubscriptionsRoute) },
                    onOpenHistory = { navController.switchTab(TopLevelDestination.HISTORY) },
                    onOpenMoney = { navController.switchTab(TopLevelDestination.MONEY) },
                )
            }
            composable<ChatRoute> { ChatScreen() }
            composable<HistoryRoute> { HistoryScreen() }
            composable<MoneyRoute> {
                MoneyScreen(
                    listState = moneyListState,
                    onOpenAccounts = { navController.navigate(AccountsRoute) },
                    onOpenPockets = { navController.navigate(PocketsRoute) },
                    onOpenDebt = { navController.navigate(DebtRoute) },
                )
            }

            composable<AccountsRoute> { AccountsScreen(onBack = back) }
            composable<AnalyticsRoute> { AnalyticsScreen(onBack = back) }
            composable<BudgetRoute> { BudgetScreen(onBack = back) }
            composable<DebtRoute> { DebtScreen(onBack = back) }
            composable<PocketsRoute> { PocketsScreen(onBack = back) }
            composable<SubscriptionsRoute> { SubscriptionsScreen(onBack = back) }
            composable<SettingsRoute> {
                SettingsScreen(
                    displayName = displayName,
                    onBack = back,
                    onSignOut = onSignOut,
                )
            }
        }
    }
}

/**
 * True while the list is at rest or moving back toward the top. Drives the FAB out of
 * the way on the way down, where it would otherwise sit over the amounts column.
 */
@Composable
private fun rememberScrollingUp(state: LazyListState): Boolean {
    var lastIndex by remember(state) { mutableIntStateOf(state.firstVisibleItemIndex) }
    var lastOffset by remember(state) { mutableIntStateOf(state.firstVisibleItemScrollOffset) }

    return remember(state) {
        derivedStateOf {
            val up = if (state.firstVisibleItemIndex == lastIndex) {
                state.firstVisibleItemScrollOffset <= lastOffset
            } else {
                state.firstVisibleItemIndex < lastIndex
            }
            lastIndex = state.firstVisibleItemIndex
            lastOffset = state.firstVisibleItemScrollOffset
            up
        }
    }.value
}

/**
 * One back stack with per-tab state saved, rather than a stack per tab: back from any
 * tab lands on Home and the next back leaves the app, which is what an Android user
 * expects. `restoreState` is what makes scroll position and filters survive a switch.
 */
private fun NavHostController.switchTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavDestination?.isTopLevel(): Boolean =
    TopLevelDestination.entries.any { destination ->
        this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
    }

/**
 * Two motions, chosen by what the move means.
 *
 * Between tabs it is a **fade-through**: the four are siblings, and sliding between them
 * would imply a hierarchy that is not there. Into a detail screen it is a **shared
 * axis** slide, because that move does have a direction — and it mirrors on the way
 * back.
 *
 * Both run on [CashetteMotion] springs, so a screen transition lands on the same physics
 * as the Expressive components inside it. Geometry overshoots; alpha never does.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isLateral(): Boolean =
    initialState.destination.isTopLevel() && targetState.destination.isTopLevel()

private fun AnimatedContentTransitionScope<NavBackStackEntry>.screenEnter(pop: Boolean): EnterTransition =
    when {
        isLateral() ->
            fadeIn(CashetteMotion.effects()) +
                scaleIn(initialScale = 0.94f, animationSpec = CashetteMotion.slowSpatial())

        else -> {
            val from = if (pop) -1 else 1
            slideInHorizontally(CashetteMotion.slowSpatial()) { it / 5 * from } +
                fadeIn(CashetteMotion.effects())
        }
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.screenExit(pop: Boolean): ExitTransition =
    when {
        isLateral() ->
            fadeOut(CashetteMotion.effects()) +
                scaleOut(targetScale = 0.98f, animationSpec = CashetteMotion.slowSpatial())

        else -> {
            val towards = if (pop) 1 else -1
            slideOutHorizontally(CashetteMotion.slowSpatial()) { it / 5 * towards } +
                fadeOut(CashetteMotion.effects())
        }
    }
