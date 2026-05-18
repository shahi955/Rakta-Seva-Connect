/**
 * Single [NavHost] for the whole app: auth, main shell (tabs), admin console, and deep-linked
 * blood request detail. Start destination is chosen once from [FirebaseAuth]; signed-in users
 * land on [AuthRoutes.HOME]. Notification deep links enqueue a request id on [RaktaApplication]
 * and are consumed here after the graph is ready.
 */
package com.raktaseva.connect.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.raktaseva.connect.ui.admin.AdminDashboardScreen
import com.raktaseva.connect.ui.admin.AdminLoginScreen
import com.raktaseva.connect.ui.auth.LoginScreen
import com.raktaseva.connect.ui.auth.RegisterScreen
import com.raktaseva.connect.ui.main.MainShell
import com.raktaseva.connect.ui.request.RequestDetailScreen

object AuthRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val ADMIN_LOGIN = "admin_login"
    const val ADMIN_HOME = "admin_home"
    const val REQUEST_DETAIL = "request_detail/{requestId}"

    fun requestDetailRoute(requestId: String) = "request_detail/$requestId"
}

@Composable
fun RaktaNavHost(
    pendingOpenRequestId: String?,
    onConsumePendingOpenRequest: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val auth = remember { FirebaseAuth.getInstance() }
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        startDestination = if (auth.currentUser != null) AuthRoutes.HOME else AuthRoutes.LOGIN
    }

    val resolvedStart = startDestination
    if (resolvedStart == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(pendingOpenRequestId, resolvedStart, navController) {
        val id = pendingOpenRequestId
        if (id.isNullOrBlank()) return@LaunchedEffect
        navController.navigate(AuthRoutes.requestDetailRoute(id)) {
            launchSingleTop = true
        }
        onConsumePendingOpenRequest()
    }

    NavHost(navController = navController, startDestination = resolvedStart) {
        composable(AuthRoutes.LOGIN) {
            LoginScreen(
                onNavigateRegister = {
                    navController.navigate(AuthRoutes.REGISTER)
                },
                onNavigateAdminLogin = {
                    navController.navigate(AuthRoutes.ADMIN_LOGIN)
                },
                onAuthenticated = {
                    navController.navigate(AuthRoutes.HOME) {
                        popUpTo(AuthRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(AuthRoutes.ADMIN_LOGIN) {
            AdminLoginScreen(
                onBack = { navController.popBackStack() },
                onAdminAuthenticated = {
                    navController.navigate(AuthRoutes.ADMIN_HOME) {
                        popUpTo(AuthRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(AuthRoutes.ADMIN_HOME) {
            AdminDashboardScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(AuthRoutes.LOGIN) {
                            launchSingleTop = true
                        }
                    }
                },
                onSignedOut = {
                    navController.navigate(AuthRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(AuthRoutes.REGISTER) {
            RegisterScreen(
                onNavigateLogin = { navController.popBackStack() },
                onRegistered = {
                    navController.navigate(AuthRoutes.HOME) {
                        popUpTo(AuthRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(AuthRoutes.HOME) {
            MainShell(
                navController = navController,
                onSignedOut = {
                    navController.navigate(AuthRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = AuthRoutes.REQUEST_DETAIL,
            arguments = listOf(
                navArgument("requestId") { type = NavType.StringType }
            )
        ) { entry ->
            val requestId = entry.arguments?.getString("requestId").orEmpty()
            RequestDetailScreen(
                requestId = requestId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
