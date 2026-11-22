package com.example.stockmarket.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.stockmarket.feature.registration.RegistrationScreen
import com.example.stockmarket.SettingsScreen
import com.example.stockmarket.data.repository.RegistrationStore
import com.example.stockmarket.data.repository.WatchlistStore
import com.example.stockmarket.feature.auth.LoginScreen
import com.example.stockmarket.feature.auth.SignUpScreen
import com.example.stockmarket.feature.history.HistoryScreen
import com.example.stockmarket.feature.home.HomeScreen
import com.example.stockmarket.feature.market.CompanyDetailScreen
import com.example.stockmarket.feature.market.MarketScreen
import com.example.stockmarket.feature.notifications.NotificationsScreen
import com.example.stockmarket.feature.portfolio.PortfolioScreen
import com.example.stockmarket.feature.profile.ProfileScreen
import com.example.stockmarket.feature.wallet.WalletScreen
import com.example.stockmarket.feature.trade.OrderScreen
import com.example.stockmarket.feature.trade.OrderStatusScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.stockmarket.data.repository.SessionManager
import com.example.stockmarket.data.repository.SessionState



private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    TabItem(Routes.HOME, "Home", Icons.Filled.Home),
    TabItem(Routes.MARKET, "Market", Icons.Filled.BarChart),
    TabItem(Routes.PORTFOLIO, "Portfolio", Icons.Filled.AccountBalanceWallet),
    TabItem(Routes.HISTORY, "History", Icons.Filled.History),
    TabItem(Routes.PROFILE, "Profile", Icons.Filled.Person),
)

// Waits until the saved login has been read, then opens the right first screen
@Composable
fun AppNavigation() {
    val session by SessionManager.state.collectAsState()

    if (session is SessionState.Loading) {
        Box(Modifier.fillMaxSize())   // the splash screen is still covering this
    } else {
        // Decided once, the first time we know the answer
        val startDestination = remember {
            if (session is SessionState.LoggedIn) Routes.HOME else Routes.LOGIN
        }
        AppNavHost(startDestination = startDestination)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppNavHost(startDestination: String) {
    val navController = rememberNavController()

// When the session ends (logout or expiry), clear the history and go to Log in
    val session by SessionManager.state.collectAsState()
    LaunchedEffect(session) {
        if (session is SessionState.LoggedOut) {
            val route = navController.currentDestination?.route
            if (route != null && route != Routes.LOGIN && route != Routes.SIGNUP) {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val registrationStatus by RegistrationStore.status.collectAsState()
    val watched by WatchlistStore.symbols.collectAsState()

    // The bottom bar shows on the five main tabs, and hides while the keyboard is open
    val imeVisible = WindowInsets.isImeVisible
    val showBottomBar = tabs.any { it.route == currentRoute } && !imeVisible

    fun goToTab(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { goToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            // padding: leave room for the status bar and bottom bar
            // consumeWindowInsets: tell the screens that space is already used
            // imePadding: lift everything above the keyboard
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            enterTransition = { fadeIn(animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(250)) },
            popEnterTransition = { fadeIn(animationSpec = tween(250)) },
            popExitTransition = { fadeOut(animationSpec = tween(250)) }
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoggedIn = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onCreateAccount = { navController.navigate(Routes.SIGNUP) }
                )
            }

            composable(Routes.SIGNUP) {
                SignUpScreen(
                    onBack = { navController.popBackStack() },
                    onSignedUp = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    onProfileClick = { goToTab(Routes.PROFILE) },
                    onRegisterClick = { navController.navigate(Routes.REGISTER) },
                    onTradeClick = { goToTab(Routes.MARKET) },
                    onWalletClick = { navController.navigate(Routes.WALLET) },
                    onCompanyClick = { symbol -> navController.navigate(Routes.company(symbol)) },
                    onSeeAllClick = { goToTab(Routes.HISTORY) }
                )
            }

            composable(Routes.MARKET) {
                MarketScreen(
                    onCompanyClick = { symbol -> navController.navigate(Routes.company(symbol)) },
                    onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    onProfileClick = { goToTab(Routes.PROFILE) }
                )
            }

            composable(Routes.PORTFOLIO) {
                PortfolioScreen(
                    onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    onProfileClick = { goToTab(Routes.PROFILE) },
                    onCompanyClick = { symbol -> navController.navigate(Routes.company(symbol)) },
                    onBrowseMarket = { goToTab(Routes.MARKET) }
                )
            }

            composable(Routes.HISTORY) {
                HistoryScreen(
                    onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    onProfileClick = { goToTab(Routes.PROFILE) }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    onProfileClick = { goToTab(Routes.PROFILE) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                    onRegisterClick = { navController.navigate(Routes.REGISTER) },
                    onLogout = { SessionManager.logout() }
                )
            }

            composable(Routes.COMPANY) { entry ->
                val symbol = entry.arguments?.getString("symbol").orEmpty()
                CompanyDetailScreen(
                    symbol = symbol,
                    registrationStatus = registrationStatus,
                    isWatched = symbol in watched,
                    onBack = { navController.popBackStack() },
                    onToggleWatch = { WatchlistStore.toggle(symbol) },
                    onTrade = { side -> navController.navigate(Routes.order(symbol, side)) },
                    onRegisterClick = { navController.navigate(Routes.REGISTER) }
                )
            }

            composable(Routes.ORDER) {
                OrderScreen(
                    onBack = { navController.popBackStack() },
                    onOrderPlaced = { orderId ->
                        navController.navigate(Routes.orderStatus(orderId)) {
                            popUpTo(Routes.ORDER) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.ORDER_STATUS) {
                OrderStatusScreen(
                    onDone = { navController.popBackStack(Routes.HOME, inclusive = false) }
                )
            }

            composable(Routes.REGISTER) {
                RegistrationScreen(
                    onBack = { navController.popBackStack() },
                    onSubmitted = { navController.popBackStack(Routes.HOME, inclusive = false) }
                )
            }

            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.WALLET) {
                WalletScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}