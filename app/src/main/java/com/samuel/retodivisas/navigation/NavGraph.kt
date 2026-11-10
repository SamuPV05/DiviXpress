package com.samuel.retodivisas.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.samuel.retodivisas.data.SessionManager
import com.samuel.retodivisas.ui.screens.AdminAuditScreen
import com.samuel.retodivisas.ui.screens.BuyCurrencyScreen
import com.samuel.retodivisas.ui.screens.HardwareScreen
import com.samuel.retodivisas.ui.screens.LoginScreen
import com.samuel.retodivisas.ui.screens.ProfileScreen
import com.samuel.retodivisas.ui.screens.RegisterScreen
import com.samuel.retodivisas.ui.screens.SellCurrencyScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val BUY = "buy_currency"
    const val SELL = "sell_currency"
    const val PROFILE = "profile"
    const val ADMIN = "admin"
    const val HARDWARE = "hardware"
}

private fun NavHostController.toLoginClearingStack() {
    navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true }; launchSingleTop = true }
}

@Composable
fun NavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    val dest = if (SessionManager.isAdmin) Routes.ADMIN else Routes.BUY
                    nav.navigate(dest) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onGoRegister = { nav.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = {
                    val dest = if (SessionManager.isAdmin) Routes.ADMIN else Routes.BUY
                    nav.navigate(dest) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.BUY) {
            SessionGuard(nav) {
                BuyCurrencyScreen(
                    onSwitchToSell = {
                        nav.navigate(Routes.SELL) {
                            popUpTo(Routes.BUY) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onProfile = { nav.navigate(Routes.PROFILE) { launchSingleTop = true } },
                    onAudit = { nav.navigate(Routes.ADMIN) { launchSingleTop = true } },
                    onHardware = { nav.navigate(Routes.HARDWARE) },
                    onLogout = { SessionManager.logout(); nav.toLoginClearingStack() }
                )
            }
        }
        composable(Routes.SELL) {
            SessionGuard(nav) {
                SellCurrencyScreen(
                    onSwitchToBuy = {
                        nav.navigate(Routes.BUY) {
                            popUpTo(Routes.BUY) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onProfile = { nav.navigate(Routes.PROFILE) { launchSingleTop = true } },
                    onAudit = { nav.navigate(Routes.ADMIN) { launchSingleTop = true } },
                    onHardware = { nav.navigate(Routes.HARDWARE) },
                    onLogout = { SessionManager.logout(); nav.toLoginClearingStack() }
                )
            }
        }
        composable(Routes.PROFILE) {
            SessionGuard(nav) {
                ProfileScreen(
                    onBack = { nav.popBackStack() },
                    onGoBuy = {
                        nav.navigate(Routes.BUY) {
                            popUpTo(Routes.BUY) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onGoSell = {
                        nav.navigate(Routes.SELL) {
                            popUpTo(Routes.BUY) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onLogout = { nav.toLoginClearingStack() }
                )
            }
        }
        composable(Routes.ADMIN) {
            SessionGuard(nav) {
                val canGoBack = nav.previousBackStackEntry != null
                AdminAuditScreen(
                    onBack = if (canGoBack) ({ nav.popBackStack() }) else null,
                    onGoToTrade = { nav.navigate(Routes.BUY) { launchSingleTop = true } },
                    onProfile = { nav.navigate(Routes.PROFILE) { launchSingleTop = true } },
                    onHardware = { nav.navigate(Routes.HARDWARE) },
                    onLogout = { nav.toLoginClearingStack() }
                )
            }
        }
        composable(Routes.HARDWARE) {
            SessionGuard(nav) { HardwareScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

@Composable
private fun SessionGuard(nav: NavHostController, content: @Composable () -> Unit) {
    if (SessionManager.currentUser == null) {
        LaunchedEffect(Unit) { nav.toLoginClearingStack() }
    } else {
        content()
    }
}
