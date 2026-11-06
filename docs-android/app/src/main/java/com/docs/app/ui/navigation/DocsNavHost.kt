package com.docs.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.docs.app.ui.history.HistoryScreen
import com.docs.app.ui.home.HomeScreen
import com.docs.app.ui.settings.SettingsScreen
import com.docs.app.ui.tool.ToolScreen

/**
 * Central navigation graph. Sub-screens receive only what they need —
 * everything is wired to the ViewModel layer, never to global state.
 */
@Composable
fun DocsNavHost(
    navController: androidx.navigation.NavHostController,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onToolClick = { toolId -> navController.navigate(Screen.Tool.create(toolId)) },
                isDark = isDark,
                onToggleTheme = onToggleTheme
            )
        }
        composable(Screen.History.route) {
            HistoryScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        composable(
            route = Screen.Tool.route,
            arguments = listOf(navArgument("toolId") { type = NavType.StringType })
        ) { entry ->
            ToolScreen(
                toolId = entry.arguments?.getString("toolId"),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
