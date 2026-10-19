package com.example.stockmarket

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // The bottom bar only shows on the five main tabs
    val showBottomBar = tabs.any { it.route == currentRoute }

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
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLogin = {
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
                    onRegisterClick = { },
                    onTradeClick = { goToTab(Routes.MARKET) },
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
                TabPlaceholder(
                    title = "Portfolio",
                    onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    onProfileClick = { goToTab(Routes.PROFILE) }
                )
            }

            composable(Routes.HISTORY) {
                TabPlaceholder(
                    title = "History",
                    onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    onProfileClick = { goToTab(Routes.PROFILE) }
                )
            }

            composable(Routes.PROFILE) {
                TabPlaceholder(
                    title = "Profile",
                    onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    onProfileClick = { goToTab(Routes.PROFILE) }
                )
            }

            composable(Routes.COMPANY) { entry ->
                val symbol = entry.arguments?.getString("symbol").orEmpty()
                CompanyDetailScreen(
                    symbol = symbol,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.NOTIFICATIONS) {
                NotificationsPlaceholder(onBack = { navController.popBackStack() })
            }
        }
    }
}