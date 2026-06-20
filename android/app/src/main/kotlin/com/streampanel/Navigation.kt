package com.streampanel

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streampanel.feature.connections.ConnectionsRoute
import com.streampanel.feature.dashboard.DashboardRoute
import com.streampanel.feature.editor.EditorRoute
import com.streampanel.feature.obs.ObsRoute
import com.streampanel.feature.settings.LayoutCustomizerRoute
import com.streampanel.feature.settings.SettingsRoute

private object Routes {
    const val Dashboard = "dashboard"
    const val Editor = "editor/{buttonId}"
    const val Settings = "settings"
    const val LayoutCustomizer = "layout-customizer"
    const val Connections = "connections"
    const val Obs = "obs"

    fun editor(buttonId: String) = "editor/$buttonId"
}

@Composable
fun StreamPanelNavHost() {
    val navController = rememberNavController()
    fun goBackToDashboard() {
        if (!navController.popBackStack()) {
            navController.navigate(Routes.Dashboard) {
                launchSingleTop = true
                popUpTo(Routes.Dashboard) { inclusive = false }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Dashboard,
    ) {
        composable(Routes.Dashboard) {
            DashboardRoute(
                onEditButton = { navController.navigate(Routes.editor(it)) },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onOpenConnections = { navController.navigate(Routes.Connections) },
                onOpenObs = { navController.navigate(Routes.Obs) },
            )
        }
        composable(
            route = Routes.Editor,
            arguments = listOf(navArgument("buttonId") { type = NavType.StringType }),
        ) {
            EditorRoute(onBack = ::goBackToDashboard)
        }
        composable(Routes.Settings) {
            SettingsRoute(
                onBack = ::goBackToDashboard,
                onOpenLayoutCustomizer = { navController.navigate(Routes.LayoutCustomizer) },
            )
        }
        composable(Routes.LayoutCustomizer) {
            LayoutCustomizerRoute(
                onBack = ::goBackToDashboard,
            )
        }
        composable(Routes.Connections) {
            ConnectionsRoute(onBack = ::goBackToDashboard)
        }
        composable(Routes.Obs) {
            ObsRoute(onBack = ::goBackToDashboard)
        }
    }
}
