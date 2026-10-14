package com.streampanel

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streampanel.feature.dashboard.DashboardRoute
import com.streampanel.feature.editor.EditorRoute
import com.streampanel.feature.obs.ObsRoute
import com.streampanel.feature.settings.LayoutCustomizerRoute
import com.streampanel.feature.settings.SettingsRoute

private object Routes {
    const val Dashboard = "dashboard"
    const val Editor = "editor/{buttonId}"
    const val Settings = "settings?page={page}"
    const val LayoutCustomizer = "layout-customizer"
    const val Obs = "obs"

    fun editor(buttonId: String) = "editor/$buttonId"

    /** Opens the settings home, or straight on one of its pages (e.g. "Connection"). */
    fun settings(page: String? = null) = if (page == null) "settings" else "settings?page=$page"
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
                onOpenSettings = { navController.navigate(Routes.settings()) },
                onOpenConnections = { navController.navigate(Routes.settings("Connection")) },
                onOpenObs = { navController.navigate(Routes.Obs) },
            )
        }
        composable(
            route = Routes.Editor,
            arguments = listOf(navArgument("buttonId") { type = NavType.StringType }),
        ) {
            EditorRoute(onBack = ::goBackToDashboard)
        }
        composable(
            route = Routes.Settings,
            arguments = listOf(
                navArgument("page") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            SettingsRoute(
                onBack = ::goBackToDashboard,
                initialPage = entry.arguments?.getString("page"),
                onOpenLayoutCustomizer = { navController.navigate(Routes.LayoutCustomizer) },
            )
        }
        composable(Routes.LayoutCustomizer) {
            LayoutCustomizerRoute(
                onBack = ::goBackToDashboard,
            )
        }
        composable(Routes.Obs) {
            ObsRoute(onBack = ::goBackToDashboard)
        }
    }
}
